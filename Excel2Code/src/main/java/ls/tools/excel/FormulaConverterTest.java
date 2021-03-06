package ls.tools.excel;


import fj.data.List;
import ls.tools.excel.model.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.BiPredicate;

import static com.google.common.base.Preconditions.checkArgument;
import static fj.data.List.list;
import static ls.tools.excel.BuiltInFunction.IF;
import static ls.tools.excel.CellType.*;
import static ls.tools.excel.model.BinaryOp.MULT;
import static ls.tools.excel.model.Functions.createFunction;
import static ls.tools.excel.model.Functions.param;
import static ls.tools.fj.Util.*;
import static org.junit.Assert.assertTrue;

public final class FormulaConverterTest implements ExpressionBuilder
{

	private static final String CUBE_SQRT = "cube_sqrt";
	private static final String B3 = "B3";
	private static final String C3 = "C3";
	private static final String D3 = "D3";
	private static final String E3 = "E3";
	private static final String CUBE = "cube";
	private static final String SQUARE = "square";
	private static final String TIMES2 = "times2";
	private static final String SHEET1 = "Sheet1";
	private static final String MULT_FUNC_NAME = "mult";
	private XSSFWorkbook _wb;
	private FormulaConverter fc;
    private final BiPredicate<Function,Function> funcEqPredicate = nullCheckingEqualPredicate();
	
	@Before 
	public void prepareTest() throws InvalidFormatException, IOException
	{
		//!Assumption: it's an openxml format (xlsx, not xls)!
		_wb = (XSSFWorkbook) WorkbookFactory.create(new FileInputStream("test.xlsx"));
		fc = new FormulaConverter();
	}
	
	@Test
	public void simple2CellMultiplication() 
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, MULT_FUNC_NAME);
		final List<Function> expected = simple2CellMultExpectedResult();
		assertTrue("Simple 2 cell multiplication comparison failed.",listsEql(result, expected, funcEqPredicate));
	}

	public List<Function> simple2CellMultExpectedResult()
	{
		return list(createFunction(MULT_FUNC_NAME, list(param(C3,NUMERIC),param(B3,NUMERIC)), 
				sequence(
						bindingOf(numericVar("_0")).to(binOp(var(B3).ofType(NUMERIC),MULT,var(C3).ofType(NUMERIC)))),
				NUMERIC));
	}
	
	


	@Test
	public void simpleScalarCellMultiplication()
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, TIMES2);
		final List<Function> expected = simpleScalarMultExpectedResult();
		assertTrue("Simple scalar multiplication comparison failed.",listsEql(result, expected, funcEqPredicate));
	}

    public List<Function> simpleScalarMultExpectedResult()
    {
        return simpleScalarMultExpectedResult("_0","_1");
    }

	private List<Function> simpleScalarMultExpectedResult(String localVar1Name, String localVar2Name)
	{
		final VarExpr localVar0 = var(localVar1Name).ofType(NUMERIC);
		return list(createFunction(TIMES2,list(param(B3,NUMERIC)),
						sequence(
								bindingOf(localVar0).to(numericLiteral(2)),
								bindingOf(numericVar(localVar2Name)).to(binOp(var(B3).ofType(NUMERIC),MULT,localVar0))),
						NUMERIC));
	}
	
	@Test
	public void singleParamUsedTwice()
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, SQUARE);
		final List<Function> expected = list(createFunction(SQUARE,list(param(B3,NUMERIC)), 
																				sequence(
																						bindingOf(numericVar("_0")).to(binOp(var(B3).ofType(NUMERIC), MULT,var(B3).ofType(NUMERIC)))), NUMERIC));
		assertTrue(listsEql(result, expected, funcEqPredicate));
	}

	@Test
	public void usingAnotherFormulaAsArgument()
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, CUBE);
		final List<Function> expectedFunctions = cubeExpectedFunctions();
		assertTrue(listsEql(result, expectedFunctions, funcEqPredicate));
	}

	private List<Function> cubeExpectedFunctions()
	{
		final VarExpr b3Var = var(B3).ofType(NUMERIC);
		final VarExpr d3Var = var(D3).ofType(NUMERIC);
		final Function sqr = createFunction(SQUARE,list(param(B3,NUMERIC)),
				sequence(bindingOf(numericVar("_0")).to(binOp(b3Var, MULT, b3Var))), NUMERIC);
		return list(sqr,
					createFunction(CUBE,list(param(B3,NUMERIC)),
												sequence(bindingOf(d3Var).to(invocationOf(sqr).withArgs(b3Var)),
															 bindingOf(numericVar("_1")).to(binOp(d3Var,MULT,b3Var))), NUMERIC));
	}
	
	
	@Test
	public void builtInFunctionOverAnotherFormula()
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, CUBE_SQRT);
		final VarExpr b3 = var(B3).ofType(NUMERIC);
		final VarExpr e3 = var(E3).ofType(NUMERIC);
		List<Function> expected = cubeExpectedFunctions();
		final Function cubeFunc = expected.last();
		final Function lastFunc = createFunction(CUBE_SQRT, list(param(B3,NUMERIC)),
												sequence(
														bindingOf(e3).to(invocationOf(cubeFunc).withArgs(b3)),
														bindingOf(numericVar("_2")).to(invocationOf(BuiltInFunction.SQRT).withArgs(e3)))
												, NUMERIC);
		expected = expected.snoc(lastFunc);
		assertTrue(listsEql(result, expected, funcEqPredicate));
	}
	
	@Test
	public void generatingFunctionsForSetOfNames()
	{
		final List<Function> result = fc.formulasFromNamedCells(workbook(),MULT_FUNC_NAME,TIMES2);
		final List<Function> expected =
                                simple2CellMultExpectedResult()
								.append(simpleScalarMultExpectedResult("_1","_2"))
								.nub();
		assertTrue(listsEql(result, expected, funcEqPredicate,true));
	}
	
	@Test
	public void convertWithoutSheetName()
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), TIMES2);
		final List<Function> expected = simpleScalarMultExpectedResult();
		assertTrue(listsEql(result, expected, funcEqPredicate));
	}
	
	@Test
	public void convertIf() throws InvalidFormatException, IOException
	{
		final XSSFWorkbook wb = (XSSFWorkbook) WorkbookFactory.create(new FileInputStream("test2.xlsx"));
		final List<Function> result = fc.formulasFromNamedCell(wb, "isEven");
		//The result's body should be:
//			_0 : NUMERIC = 2;
//			_1 : NUMERIC = MOD(B3 : NUMERIC,_0 : NUMERIC);
//			_2 : NUMERIC = 0;
//			_3 : BOOLEAN = (_1 : NUMERIC) = (_2 : NUMERIC);
//			_4 : BOOLEAN = TRUE;
//			_5 : BOOLEAN = FALSE;
//			_6 : FORMULA = IF(_3 : BOOLEAN,_4 : BOOLEAN,_5 : BOOLEAN);
		
		//Let's build it explicitly:
		final VarExpr _B3 = var(B3).ofType(NUMERIC); //The parameter to the function
		final Binding _0 = bindingOf(numericVar("_0")).to(numericLiteral(2));
		final Binding _1 = bindingOf(numericVar("_1")).to(invocationOf(BuiltInFunction.MOD).withArgs(_B3,_0.var()));
		final Binding _2 = bindingOf(numericVar("_2")).to(numericLiteral(0));
		final Binding _3 = bindingOf(booleanVar("_3")).to(binOp(_1.var(), BinaryOp.EQL, _2.var()));
		final Binding _4 = bindingOf(booleanVar("_4")).to(booleanLiteral(true));
		final Binding _5 = bindingOf(booleanVar("_5")).to(booleanLiteral(false));
		final Binding _6 = bindingOf(var("_6").ofType(FORMULA)).to(invocationOf(IF).withArgs(_3.var(),_4.var(),_5.var()));
		
		final List<Function> expected = list(createFunction("isEven",list(param(B3, NUMERIC)),
												sequence(_0,_1,_2,_3,_4,_5,_6),
												FORMULA));
		assertTrue(listsEql(result, expected, funcEqPredicate));
	}

	private VarExpr booleanVar(final String varName)
	{
		checkArgument(notEmpty(varName), "Can't have an empty variable name");
		return var(varName).ofType(BOOLEAN);
	}
	
	private VarExpr numericVar(final String varName)
	{
		checkArgument(notEmpty(varName), "Can't have an empty variable name");
		return var(varName).ofType(NUMERIC);
	}
	
	private XSSFWorkbook workbook() { return _wb; }

}
