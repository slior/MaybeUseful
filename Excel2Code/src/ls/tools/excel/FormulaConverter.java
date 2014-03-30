package ls.tools.excel;


import fj.data.List;
import fj.data.Option;
import ls.tools.excel.model.*;
import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Stack;

import static com.google.common.base.Preconditions.*;
import static fj.Equal.equal;
import static fj.data.List.list;
import static fj.data.List.nil;
import static ls.tools.excel.CellType.FORMULA;
import static ls.tools.excel.CellType.fromSSCellType;
import static ls.tools.excel.model.Functions.createFunction;
import static ls.tools.excel.model.Functions.param;
import static ls.tools.fj.Util.fj;
import static ls.tools.fj.Util.notEmpty;
import static org.apache.poi.ss.formula.FormulaParser.parse;

public final class FormulaConverter implements ExpressionBuilder
{

	private static final int RESOLVE_NAMES_IN_CONTAINING_SHEET = -1;
	private final Stack<Expr> resultStack = new Stack<Expr>();
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
		final Name _name = wb.getName(name);
		return formulasFromNamedCell(wb, sheetName, _name);
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
		final Expr body = sequence(bodySeq);
		return generatedFunctions.snoc(createFunction(name,paramList(),body,body.type()));
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
				final Binding b = createBindingTo(binOp(evaluationOf(op1), op(token), evaluationOf(op2)));
				resultStack.push(addToBody(b));
			}
			else if (isFuncCall(token))
			{
				final Binding b = createBindingToFunctionResult((RefPtg) token);
				resultStack.push(evaluationOf(addToBody(b)));
			}
			else if (isBuiltInFunction(token))
			{
				final Function builtIn = builtInFunction(((AbstractFunctionPtg)token).getName());
                final List<Expr> args = builtIn.parameters()
                                            .map(fj(p -> evaluationOf(resultStack.pop())))
                                            .reverse();
				final FunctionExpr fe = invocationOf(builtIn).withArgs(args.toArray().array(Expr[].class));
				final Binding b = createBindingTo(fe);
				resultStack.push(addToBody(b));
			}
			else if (isCellReference(token))
			{
				unresolvedSymbols = unresolvedSymbols.cons((RefPtg)token);
				resultStack.push(var(token.toFormulaString()).ofType(typeOfCellReferencedBy((RefPtg)token)));
			}
		}
		
	}

	private Expr evaluationOf(final Expr e)
	{
		if (e instanceof Binding) //TODO: this distinction should be part of the expression interface - encapsulation
			return ((Binding)e).var();
		else 
			return e;
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
		return token instanceof AbstractFunctionPtg;
	}


	private Binding createBindingToLiteral(Ptg token)
	{
		checkArgument(token instanceof ScalarConstantPtg,"Illegal token for literal - should be a scalar");
		final CellType type = CellType.literalTypeFrom((ScalarConstantPtg)token);
		return createBindingTo(literal(token.toFormulaString()).ofType(type));
	}

	private Binding createBindingTo(final Expr e)
	{
		return bindingOf(var(newLocalVarName()).ofType(e.type())).to(e);
	}
	
	private boolean isCellReference(Ptg token)
	{
		return token instanceof RefPtg && !typeOfCellReferencedBy((RefPtg)token).equals(FORMULA);
	}

	private BinaryOp op(final Ptg token) //TODO: should unify this definition with that of #isBinaryOp
	{ 
		if (token instanceof MultiplyPtg) return BinaryOp.MULT;
		else if (token instanceof EqualPtg) return BinaryOp.EQL;
		else throw new IllegalArgumentException("Can't resolve operator for token: " + token.toFormulaString());
	}

	private boolean isFuncCall(Ptg token)
	{
		return token instanceof RefPtg && typeOfCellReferencedBy((RefPtg)token).equals(FORMULA);
	}

	private boolean isBinaryOp(final Ptg token) 
	{ 
		return (token instanceof MultiplyPtg) || (token instanceof EqualPtg); 
	}

	private boolean isLiteral(final Ptg token) 
	{ 
		return (token instanceof IntPtg) || (token instanceof BoolPtg); 
	}


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
                .nub(equal(fj((RefPtg r1) -> fj(((RefPtg r2) -> r1.toFormulaString().equals(r2.toFormulaString()))))))
				//Filter out all the formula references
                .filter(fj(ref -> typeOfCellReferencedBy(ref) != FORMULA))
				//Convert references to param declarations
                .map(fj(ref->param(ref.toFormulaString(),typeOfCellReferencedBy(ref)))
                );
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
        //map all parameters to an argument to pass to the invocation. We assume they're defined, probably as arguments.
        final List<VarExpr> args = funcToInvoke.parameters().map(fj(p -> var(p.name(),p.type())));
		final VarExpr newVar = var(token.toFormulaString(), f.last().returnType());
		return bindingOf(newVar).to(invocationOf(f.last()).withArgs(args.toArray().array(VarExpr[].class)));
	}


	/**
	 * Add the given expression to the body of the current function being created.
	 * @param expr The expression to add
	 * @return The expression given at input
	 */
	//TODO: code smell?
	private Expr addToBody(final Expr expr)
	{
		checkArgument(expr != null,"expression can't be null in function body");
		bodySeq  = bodySeq.snoc(expr);
		return expr;
	}


	private VarExpr var(final String varName, final CellType varType)
	{
		return var(varName).ofType(varType);
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
        return namedRangesIn(sheet.getWorkbook())
                .find(fj(n -> c.equals(cell(n.getRefersToFormula()))));
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
                    .map(fj(name -> {
                        final List<Function> ret = formulasFromNamedCell(workbook, name);
                        clearUnresolvedSymbols();
                        return ret;
                    }))
					//concatenate all the results together
                    .foldLeft(fj((a, b) -> a.append(b)), initial)
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
