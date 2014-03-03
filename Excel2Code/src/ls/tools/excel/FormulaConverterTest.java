package ls.tools.excel;


import static fj.data.List.list;
import static ls.tools.excel.CellType.NUMERIC;
import static ls.tools.excel.model.BinaryOp.MULT;
import static ls.tools.excel.model.ExprBuilder.e;
import static ls.tools.excel.model.Functions.createFunction;
import static ls.tools.excel.model.Functions.param;
import static ls.tools.fj.Util.listsEqual;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import ls.tools.excel.model.Function;
import ls.tools.excel.model.VarExpr;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;

import fj.F2;
import fj.data.List;

public final class FormulaConverterTest 
{

	private static final String CUBE_SQRT = "cube_sqrt";
//	private static final String MULT_OP = "*";
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
	private final F2<Function, Function, Boolean> funcEqPredicate = new FunctionEqlPredicate();
	
	@Before 
	public void prepareTest() throws InvalidFormatException, FileNotFoundException, IOException
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
		assertTrue("Simple 2 cell multiplication comparison failed.",listsEqual(result, expected, funcEqPredicate ));
	}

	public List<Function> simple2CellMultExpectedResult()
	{
		return list(createFunction(MULT_FUNC_NAME, list(param(C3,NUMERIC),param(B3,NUMERIC)), 
				e().sequence(
//						e().binOp(MULT_OP).ofType(NUMERIC).andOperands(
//							e().var(B3).ofType(NUMERIC), 
//							e().var(C3).ofType(NUMERIC))), NUMERIC));
						e().binOp(e().var(B3).ofType(NUMERIC),MULT,e().var(C3).ofType(NUMERIC))),
				NUMERIC));
	}
	
	


	@Test
	public void simpleScalarCellMultiplication()
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, TIMES2);
		final List<Function> expected = simpleScalarMultExpectedResult();
		assertTrue("Simple scalar multiplication comparison failed.",listsEqual(result, expected, funcEqPredicate ));
	}

	public List<Function> simpleScalarMultExpectedResult()
	{
		final VarExpr localVar0 = e().var("_0").ofType(NUMERIC);
		return list(createFunction(TIMES2,list(param(B3,NUMERIC)),
																	e().sequence(
																			e().bindingOf(localVar0).to(e().literal("2").ofType(NUMERIC)),
//																			e().binOp(MULT_OP).ofType(NUMERIC).andOperands(e().var(B3).ofType(NUMERIC),localVar0)),
																			e().binOp(e().var(B3).ofType(NUMERIC),MULT,localVar0)),
																	NUMERIC));
	}
	
	@Test
	public void singleParamUsedTwice()
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, SQUARE);
		final List<Function> expected = list(createFunction(SQUARE,list(param(B3,NUMERIC)), 
																				e().sequence(e().binOp(e().var(B3).ofType(NUMERIC), MULT,e().var(B3).ofType(NUMERIC))), NUMERIC));
//																						e().binOp(MULT_OP).ofType(NUMERIC)
//																							.andOperands(
//																								e().var(B3).ofType(NUMERIC), 
//																								e().var(B3).ofType(NUMERIC))), NUMERIC));
		assertTrue(listsEqual(result, expected, funcEqPredicate));
	}

	@Test
	public void usingAnotherFormulaAsArgument()
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, CUBE);
		final List<Function> expectedFunctions = cubeExpectedFunctions();
		assertTrue(listsEqual(result, expectedFunctions, funcEqPredicate));
	}

	private static List<Function> cubeExpectedFunctions()
	{
		final VarExpr b3Var = e().var(B3).ofType(NUMERIC);
		final VarExpr d3Var = e().var(D3).ofType(NUMERIC);
		return list(createFunction(SQUARE,list(param(B3,NUMERIC)),
												e().sequence(e().binOp(b3Var, MULT, b3Var)), NUMERIC),
					createFunction(CUBE,list(param(B3,NUMERIC)),
												e().sequence(e().bindingOf(d3Var).to(e().invocationOf(SQUARE).ofType(NUMERIC).withArgs(b3Var)),
															 e().binOp(d3Var,MULT,b3Var)), NUMERIC));
	}
	
	
	@Test
	public void builtInFunctionOverAnotherFormula()
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, CUBE_SQRT);
		final VarExpr b3 = e().var(B3).ofType(NUMERIC);
		final VarExpr e3 = e().var(E3).ofType(NUMERIC);
		
		final Function lastFunc = createFunction(CUBE_SQRT, list(param(B3,NUMERIC)),
												e().sequence(
														e().bindingOf(e3).to(e().invocationOf(CUBE).ofType(NUMERIC).withArgs(b3)),
														e().invocationOf(BuiltInFunction.SQRT).withArgs(e3))
												, NUMERIC);
		final List<Function> expected = cubeExpectedFunctions().snoc(lastFunc);
		assertTrue(listsEqual(result, expected, funcEqPredicate));
	}
	
	@Test
	public void generatingFunctionsForSetOfNames()
	{
		final List<Function> result = fc.formulasFromNamedCells(workbook(),MULT_FUNC_NAME,TIMES2);
		final List<Function> expected = 
								simple2CellMultExpectedResult()
								.append(simpleScalarMultExpectedResult())
								.nub();
		assertTrue(listsEqual(result, expected, funcEqPredicate));
	}
	
	@Test
	public void convertWithoutSheetName()
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), TIMES2);
		final List<Function> expected = simpleScalarMultExpectedResult();
		assertTrue(listsEqual(result, expected, funcEqPredicate));
	}
	
	private XSSFWorkbook workbook() 
	{
		return _wb;
	}

}
