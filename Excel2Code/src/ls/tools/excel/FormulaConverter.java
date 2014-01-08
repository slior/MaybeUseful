package ls.tools.excel;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static fj.Equal.equal;
import static fj.data.List.nil;
import static ls.tools.excel.CellType.FORMULA;
import static ls.tools.excel.CellType.NUMERIC;
import static ls.tools.excel.CellType.fromSSCellType;
import static ls.tools.excel.FunctionImpl.param;
import static ls.tools.excel.model.ExprBuilder.e;
import static org.apache.poi.ss.formula.FormulaParser.parse;

import java.util.Stack;

import ls.tools.excel.model.Binding;
import ls.tools.excel.model.Expr;
import ls.tools.excel.model.Param;
import ls.tools.excel.model.VarExpr;

import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.IntPtg;
import org.apache.poi.ss.formula.ptg.MultiplyPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.RefPtg;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import fj.F;
import fj.data.List;
import fj.data.Option;

public final class FormulaConverter
{

	private static final int RESOLVE_NAMES_IN_CONTAINING_SHEET = -1;
	private final Stack<Expr> resultStack = new Stack<>();
	private List<RefPtg> unresolvedSymbols = nil();
	private XSSFSheet sheet;
	private FormulaParsingWorkbook fpwb;
	private List<Function> generatedFunctions = nil();
	private List<Expr> bodySeq = nil();
	private int localVarCount = 0;
	
	List<Function> formulasFromNamedCell(final XSSFWorkbook wb, final String sheetName, final String name)
	{
		final Name n = wb.getName(name);
		final CellReference cr = new CellReference(n.getRefersToFormula());
		sheet = wb.getSheet(sheetName);
		fpwb = XSSFEvaluationWorkbook.create(wb);
		final Cell c = sheet.getRow(cr.getRow()).getCell(cr.getCol());
		
		final String formula = c.getCellFormula();
		return convertFormulaToFunction(name, formula);
	}


	private List<Function> convertFormulaToFunction(final String name, final String formula)
	{
		checkState(fpwb != null,"Formula parsing workbook must be resolve for parsing a formula");
		
		final Ptg[] tokens = parse(formula, fpwb, FormulaType.CELL, RESOLVE_NAMES_IN_CONTAINING_SHEET);
		generateExpressionsForTokens(tokens);
		return createFunction(name);
	}


	/**
	 * Create and return the function out of the current state of {@link #bodySeq}.
	 * It also appends all the {@link #generatedFunctions}, that were recursively generated during the transformation of this function.
	 * At the end, all the {@link #bodySeq} and {@link #generatedFunctions} and the {@link #resultStack} are cleared.
	 * The last function created is the last in the list (the one with the given name).
	 * @param name The name of the function to create.
	 * @return The newly create function, with any recursively created functions.
	 */
	private List<Function> createFunction(final String name)
	{
		final Expr body = e().sequence(bodySeq);
		clearBodySeq();
		clearResultStack();
		final List<Function> ret = generatedFunctions.snoc(FunctionImpl.create(name,paramList(),body,body.type()));
		clearGeneratedFunctions();
		return ret;
	}

	/**
	 * Given a list of tokens, in reverse polish notation, go over all of them, and create necessary expressions for all of them.
	 * This will update the {@link #bodySeq} expression sequence, and possibly the {@link #generatedFunctions} list, if any other functions are created.
	 * @param tokens The formula tokens to convert from.
	 */
	private void generateExpressionsForTokens(final Ptg[] tokens)
	{
		//tokens are in RPN.
		for (Ptg token : tokens)
		{
			if (token instanceof RefPtg)
				handleReference((RefPtg)token);
			else if (token instanceof MultiplyPtg)
				handleMultOp((MultiplyPtg)token);
			else if (token instanceof IntPtg)
				handleIntLiteral((IntPtg)token);
		}
	}

	private void clearBodySeq() { bodySeq = nil(); }
	private void clearResultStack() { resultStack.clear(); }
	private void clearGeneratedFunctions() { generatedFunctions = nil(); }
	
	private void handleIntLiteral(final IntPtg token)
	{
		final Binding b = e().bindingOf(e().var(newLocalVarName()).ofType(NUMERIC)).to(e().literal(token.toFormulaString()).ofType(NUMERIC));
		addToBody(b);
//		resultStack.push(b.var());
	}


	//Not thread safe - does it need to be?
	private String newLocalVarName() { return "_" + (localVarCount ++); }


	private Cell cell(final String cellFormula)
	{
		checkState(sheet != null,"Sheet cannot be null");
		final CellReference cr = new CellReference(cellFormula);
		return sheet.getRow(cr.getRow()).getCell(cr.getCol());
	}
	
	private List<Param> paramList()
	{
		return unresolvedSymbols
				//Remove duplicates
				.nub(equal(new F<RefPtg, F<RefPtg,Boolean>>() {
					@Override public F<RefPtg, Boolean> f(final RefPtg r1) {
						return new F<RefPtg, Boolean>() { @Override public Boolean f(final RefPtg r2) { return r1.toFormulaString().equals(r2.toFormulaString()); } };
					}
				})) 
				//Filter out all the formula references
				.filter(new F<RefPtg,Boolean>() { @Override public Boolean f(RefPtg a) { return typeOfCellReferencedBy(a) != CellType.FORMULA; } })
				//Convert references to param declarations
				.map(new F<RefPtg,Param>() { @Override public Param f(final RefPtg token) { return param(token.toFormulaString(),typeOfCellReferencedBy(token)); }});
	}
	
	private void handleMultOp(final MultiplyPtg token)
	{
//		checkState(resultStack.size() >= 2,"Must have at least 2 operands for multiplication");
//		final Expr op2 = resultStack.pop();
//		final Expr op1 = resultStack.pop();
//		resultStack.push(addToBody(e().binOp("*").ofType(NUMERIC).andOperands(op1,op2)));
		
	}

	private void handleReference(final RefPtg token)
	{
		if (typeOfCellReferencedBy(token).equals(FORMULA))
//			resultStack.push(generateFunctionAndInvocation(token));
			addToBody(generateFunctionAndInvocation(token));
		else
		{
			unresolvedSymbols = unresolvedSymbols.cons(token);
//			resultStack.push(e().var(token.toFormulaString()).ofType(typeOfCellReferencedBy(token)));
			addToBody(e().var(token.toFormulaString()).ofType(typeOfCellReferencedBy(token)));
		}
	}


	/**
	 * Given the token that points to a formula cell, generate:
	 * <ol>
	 * <li>The formula in that cell (and recursively any others)</li>
	 * <li>The call to that formula</li>
	 * <li>A binding of a new variable to the result of the function invocation</li>
	 * </ol> 
	 * It returns the newly created variable, bound to the result of the generated function call.
	 * This also updated {@link #generatedFunctions} and {@link #bodySeq}, with the new functions and statements.
	 * @param token The token referencing a formula cell.
	 * @return The newly created variable, bound to the result of the function call, from the newly generated function.
	 */
	private VarExpr generateFunctionAndInvocation(final RefPtg token)
	{
		final Cell c = cell(token.toFormulaString());
		final Option<Name> n = nameForCell(c);
		final String name = n.isSome() ? n.valueE("No name").getNameName() : token.toFormulaString();
		final List<Function> f = convertFormulaToFunction(name, c.getCellFormula()); 
		rememberFunctions(f);
		//generate the invocation code
		//Assumption: the last function is the one we need to work with.
		final Function funcToInvoke = f.last();
		final List<VarExpr> args = funcToInvoke.parameters().map( //map all parameters to an argument to pass to the invocation. We assume they're defined, probably as arguments.
				new F<Param,VarExpr>() { @Override public VarExpr f(final Param a) { return e().var(a.name()).ofType(a.type()); }});
		
		final VarExpr newVar = var(token.toFormulaString(), f.last().returnType());
		addToBody(e().bindingOf(newVar).to(e().invocationOf(f.last()).withArgs(args.toArray().array(VarExpr[].class))));
		return newVar;
	}


	/**
	 * Add the given expression to the body of the current function being created.
	 * @param expr The expression to add
	 * @return The expression given at input
	 */
	private Expr addToBody(final Expr expr)
	{
		checkArgument(expr != null,"expression can't be null in function body");
		bodySeq  = bodySeq.snoc(expr);
		return expr;
	}


	private VarExpr var(final String varName, final CellType varType)
	{
		return e().var(varName).ofType(varType);
	}

	private void rememberFunctions(final List<Function> f)
	{
		generatedFunctions = generatedFunctions .append(f);
	}
	
	/**
	 * Given a cell, will find and return a name pointing to that cell, if it exists.
	 * @param c The cell for which we're looking for a name.
	 * @return Some(name), if a name is present, None if no such name exists.
	 */
	private Option<Name> nameForCell(final Cell c)
	{
		final Workbook wb = sheet.getWorkbook();
		final List<Name> names = namedRangesIn(wb);
		return names.find(new F<Name,Boolean>() {
			@Override public Boolean f(final Name n) { return c.equals(cell(n.getRefersToFormula())); }});
	}
	

	private List<Name> namedRangesIn(final Workbook wb)
	{
		List<Name> ret = List.nil();
		for (int i = wb.getNumberOfNames()-1; i >=0 ; i--)
			ret = ret.cons(wb.getNameAt(i));
		return ret;
	}

	private CellType typeOfCellReferencedBy(final RefPtg ref)
	{
		return fromSSCellType(cell(ref.toFormulaString()).getCellType());
	}

}
