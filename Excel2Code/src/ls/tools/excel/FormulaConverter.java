package ls.tools.excel;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static fj.Equal.equal;
import static fj.data.List.list;
import static fj.data.List.nil;
import static ls.tools.excel.CellType.FORMULA;
import static ls.tools.excel.CellType.NUMERIC;
import static ls.tools.excel.CellType.fromSSCellType;
import static ls.tools.excel.FunctionImpl.param;
import static ls.tools.excel.model.ExprBuilder.e;
import static ls.tools.fj.Util.notEmpty;
import static org.apache.poi.ss.formula.FormulaParser.parse;

import java.util.Stack;

import ls.tools.excel.model.Binding;
import ls.tools.excel.model.Expr;
import ls.tools.excel.model.FunctionExpr;
import ls.tools.excel.model.Param;
import ls.tools.excel.model.VarExpr;

import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.FuncPtg;
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
import fj.F2;
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
	
	/**
	 * Given a workbook and a named cell in a given sheet, create and retrieve the functions referenced by the named cell, and all functions it depends on.
	 * @param wb The workbook the functions are in
	 * @param sheetName The name of the sheet containing the named cell
	 * @param name The name of the named cell.
	 * @return The list of functions parsed from the given named cell, and all functions it depends on.
	 */
	List<Function> formulasFromNamedCell(final XSSFWorkbook wb, final String sheetName, final String name)
	{
		final Name n = wb.getName(name);
		return formulasFromNamedCell(wb, sheetName, n);
	}


	private List<Function> formulasFromNamedCell(final XSSFWorkbook wb, final String sheetName, final Name n)
	{
		checkArgument(wb != null,"Workbook can't be null");
		checkArgument(notEmpty(sheetName),"Sheet name can't be empty");
		checkArgument(n != null,"Named cell can't be null");
		
		final CellReference cr = new CellReference(n.getRefersToFormula());
		sheet = wb.getSheet(sheetName);
		fpwb = XSSFEvaluationWorkbook.create(wb);
		final Cell c = sheet.getRow(cr.getRow()).getCell(cr.getCol());
		
		final String formula = c.getCellFormula();
		return convertFormulaToFunction(n.getNameName(), formula);
	}


	private List<Function> convertFormulaToFunction(final String name, final String formula)
	{
		checkState(fpwb != null,"Formula parsing workbook must be resolve for parsing a formula");
		
		final Ptg[] tokens = parse(formula, fpwb, FormulaType.CELL, RESOLVE_NAMES_IN_CONTAINING_SHEET);
		generateExpressionsForTokens(tokens);
		final List<Function> ret = createFunctionsFor(name);
		clearState();
		return ret;
	}


	/**
	 * Clear the {@link #bodySeq body}, the {@link #resultStack result stack} and {@link #generatedFunctions generated functions} list
	 */
	private void clearState()
	{
		clearBodySeq();
		clearResultStack();
		clearGeneratedFunctions();
	}


	/**
	 * Create and return the function out of the current state of {@link #bodySeq}.
	 * It also appends all the {@link #generatedFunctions}, that were recursively generated during the transformation of this function.
	 * <br/>
	 * The last function created is the last in the list (the one with the given name).
	 * @param name The name of the function to create.
	 * @return The newly create function, with any recursively created functions.
	 */
	private List<Function> createFunctionsFor(final String name)
	{
		final Expr body = e().sequence(bodySeq);
		return generatedFunctions.snoc(FunctionImpl.create(name,paramList(),body,body.type()));
	}

	


	/**
	 * Given a list of tokens, in reverse polish notation, go over all of them, and create necessary expressions for all of them.
	 * This will update the {@link #bodySeq body} expression sequence, and possibly the {@link #generatedFunctions} list, if any other functions are created.
	 * <p>
	 * This is basically the "heart" of the conversion process. All tokens are converted to expressions, pushed to a {@link #resultStack stack}, and popped when necessary.
	 * all the stack handling (should) take(s) place in this function alone.
	 * </p>
	 * @param tokens The formula tokens to convert from.
	 */
	private void generateExpressionsForTokens(final Ptg[] tokens)
	{
		//tokens are in RPN.
		for (Ptg token : tokens)
		{
			if (isLiteral(token))
			{
				final Binding b = createBindingToLiteral(token);
				addToBody(b);
				resultStack.push(b.var());
			}
			else if (isBinaryOp(token))
			{
				if (resultStack.size() < 2) throw new IllegalStateException("Binary operator must have at least two operands.");
				final Expr op2 = resultStack.pop();
				final Expr op1 = resultStack.pop();
				resultStack.push(addToBody(e().binOp(op(token)).ofType(NUMERIC).andOperands(op1,op2)));
			}
			else if (isFuncCall(token))
			{
				final Binding b = createBindingToFunctionResult((RefPtg) token);
				addToBody(b);
				resultStack.push(b.var());
			}
			else if (isBuiltInFunction(token))
			{
				final Function builtIn = builtInFunction(((FuncPtg)token).getName());
				final List<VarExpr> args =  builtIn.parameters().map(new F<Param,VarExpr>() {
					@Override
					public VarExpr f(final Param a)
					{
						final Expr e = resultStack.pop(); //note: this changes state of algorithm, the stack is popped.
						checkState(e instanceof VarExpr, "Arguments to built-in function must be variables. Found another expression on the stack: " + e.toString());
						return (VarExpr)e;
					}});
				
				//TODO: produce a local variable and binding for nested function calls?
				final FunctionExpr fe = e().invocationOf(builtIn).withArgs(args.toArray().array(VarExpr[].class));
				addToBody(fe);
				resultStack.push(fe);
			}
			else if (isCellReference(token))
			{
				unresolvedSymbols = unresolvedSymbols.cons((RefPtg)token);
				resultStack.push(e().var(token.toFormulaString()).ofType(typeOfCellReferencedBy((RefPtg)token)));
			}
		}
		
	}


	private Function builtInFunction(final String funcName)
	{
		checkArgument(funcName != null, "Built in function name can't be null when searching for its metadata");
		final Function f = BuiltInFunction.valueOf(funcName);
		return checkNotNull(f, "Couldn't find built in function with name = " + funcName);
	}


	private boolean isBuiltInFunction(final Ptg token)
	{
		checkArgument(token != null,"Can't answer for a null token - is null a built in function?");
		return token instanceof FuncPtg;
	}


	private Binding createBindingToLiteral(Ptg token)
	{
		return e().bindingOf(e().var(newLocalVarName()).ofType(NUMERIC)).to(e().literal(token.toFormulaString()).ofType(NUMERIC));
	}

	
	private boolean isCellReference(Ptg token)
	{
		if (!(token instanceof RefPtg)) return false;
		return !typeOfCellReferencedBy((RefPtg)token).equals(FORMULA);
	}


	private String op(final Ptg token)
	{
		if (token instanceof MultiplyPtg) return "*";
		else throw new IllegalArgumentException("Can't resolve operator for token: " + token.toFormulaString());
	}


	private boolean isFuncCall(Ptg token)
	{
		if (!(token instanceof RefPtg)) return false;
		return typeOfCellReferencedBy((RefPtg)token).equals(FORMULA); 
	}

	private boolean isBinaryOp(final Ptg token) { return token instanceof MultiplyPtg; }

	private boolean isLiteral(final Ptg token) { return token instanceof IntPtg; }


	private void clearBodySeq() { bodySeq = nil(); }
	private void clearResultStack() { resultStack.clear(); }
	private void clearGeneratedFunctions() { generatedFunctions = nil(); }
	private void clearUnresolvedSymbols() { unresolvedSymbols = nil(); }
	
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

	/**
	 * Given the token that points to a formula cell, generate:
	 * <ol>
	 * <li>The formula in that cell (and recursively any others)</li>
	 * <li>The call to that formula</li>
	 * <li>A binding of a new variable to the result of the function invocation</li>
	 * </ol> 
	 * It returns the newly created binding of a new variable, bound to the result of the generated function call.
	 * This also updated {@link #generatedFunctions} and {@link #bodySeq}, with the new functions and statements.
	 * @param token The token referencing a formula cell.
	 * @return The newly created binding expression, with the new variable bound to the result of the function call, from the newly generated function.
	 */
	private Binding createBindingToFunctionResult(final RefPtg token)
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
		return e().bindingOf(newVar).to(e().invocationOf(f.last()).withArgs(args.toArray().array(VarExpr[].class)));
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


	/**
	 * For the given set of names, generate the necessary functions (and all dependent ones), and return them.
	 * @param workbook The workbook containing the names
	 * @param names The set of names to convert.
	 * @return The list of functions converted from formulas in the given cells.
	 * @see #formulasFromNamedCell(XSSFWorkbook, String, String)
	 */
	public List<Function> formulasFromNamedCells(final XSSFWorkbook workbook,final String... names)
	{
		final List<Function> initial = nil();
		return list(names)
					//convert each name to a list of functions. Note: clears the unresolved symbols after each mapping.
					.map(new F<String,List<Function>>() { @Override public List<Function> f(final String name) { 
						final List<Function> ret = formulasFromNamedCell(workbook, name); 
						clearUnresolvedSymbols(); 
						return ret;
						}})
					//concatenate all the results together
					.foldLeft(new F2<List<Function>,List<Function>,List<Function>>() {@Override public List<Function> f(List<Function> a,List<Function> b) {
						return a.append(b); 
						}},initial)
					//and remove duplicates
					.nub();
	}

	List<Function> formulasFromNamedCell(final XSSFWorkbook workbook, final String name)
	{
		checkArgument(workbook != null,"Source workbook can't be null ");
		checkArgument(name != null,"Name of named cell can't be null");
		final Name n = workbook.getName(name);
		checkNotNull(n, "Couldn't find name: " + name);
		return formulasFromNamedCell(workbook, n.getSheetName(), name);
	}

}
