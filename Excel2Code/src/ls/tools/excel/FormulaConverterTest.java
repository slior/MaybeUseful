package ls.tools.excel;


import static fj.data.List.list;
import static junit.framework.Assert.assertTrue;
import static ls.tools.excel.CellType.NUMERIC;
import static ls.tools.excel.FunctionImpl.param;
import static ls.tools.excel.model.ExprBuilder.e;
import static ls.tools.fj.Util.listsEqual;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

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
	private static final String MULT_OP = "*";
	private static final String C3 = "C3";
	private static final String B3 = "B3";
	private static final String CUBE = "cube";
	private static final String SQUARE = "square";
	private static final String TIMES2 = "times2";
	private static final String SHEET1 = "Sheet1";
	private static final String MULT_FUNC_NAME = "mult";
	private XSSFWorkbook _wb;
	private FormulaConverter fc;
	private final F2<Function, Function, Boolean> funcEqPredicate = new F2<Function, Function, Boolean>() { 
		@Override public Boolean f(Function a, Function b) 
		{
			if (a == null) return b == null;
			final boolean ret = a.equals(b);
			return ret;
		} 
	};
	
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
		final List<Function> expected = list(FunctionImpl.create(MULT_FUNC_NAME, list(param(C3,NUMERIC),param(B3,NUMERIC)), 
				e().sequence(
						e().binOp(MULT_OP).ofType(NUMERIC).andOperands(
							e().var(B3).ofType(NUMERIC), 
							e().var(C3).ofType(NUMERIC))), NUMERIC));
		assertTrue("Simple 2 cell multiplication comparison failed.",listsEqual(result, expected, funcEqPredicate ));
	}
	
	


	@Test
	public void simpleScalarCellMultiplication() throws Exception
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, TIMES2);
		final VarExpr localVar0 = e().var("_0").ofType(NUMERIC);
		final List<Function> expected = list(FunctionImpl.create(TIMES2,list(param(B3,NUMERIC)),
																	e().sequence(
																			e().bindingOf(localVar0).to(e().literal("2").ofType(NUMERIC)),
																			e().binOp(MULT_OP).ofType(NUMERIC).andOperands(e().var(B3).ofType(NUMERIC),localVar0)), 
																	NUMERIC));
		assertTrue("Simple scalar multiplication comparison failed.",listsEqual(result, expected, funcEqPredicate ));
	}
	
	@Test
	public void singleParamUsedTwice() throws Exception
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, SQUARE);
		final List<Function> expected = list(FunctionImpl.create(SQUARE,list(param(B3,NUMERIC)), 
																				e().sequence(
																						e().binOp(MULT_OP).ofType(NUMERIC)
																							.andOperands(
																								e().var(B3).ofType(NUMERIC), 
																								e().var(B3).ofType(NUMERIC))), NUMERIC));
		assertTrue(listsEqual(result, expected, funcEqPredicate));
	}

	@Test
	public void usingAnotherFormulaAsArgument() throws Exception
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, CUBE);
		final List<Function> expectedFunctions = cubeExpectedFunctions();
		assertTrue(listsEqual(result, expectedFunctions, funcEqPredicate));
	}

	private static List<Function> cubeExpectedFunctions()
	{
		final VarExpr b3Var = e().var(B3).ofType(NUMERIC);
		final VarExpr d3Var = e().var("D3").ofType(NUMERIC);
		return list(FunctionImpl.create(SQUARE,list(param(B3,NUMERIC)),
												e().sequence(e().binOp(MULT_OP).ofType(NUMERIC).andOperands(b3Var, b3Var)), NUMERIC),
					FunctionImpl.create(CUBE,list(param(B3,NUMERIC)),
												e().sequence(e().bindingOf(d3Var).to(e().invocationOf(SQUARE).ofType(NUMERIC).withArgs(b3Var)),
															 e().binOp(MULT_OP).ofType(NUMERIC).andOperands(d3Var,b3Var)), NUMERIC));
	}
	
	
	@Test
	public void builtInFunctionOverAnotherFormula() throws Exception
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, CUBE_SQRT);
		final VarExpr b3 = e().var(B3).ofType(NUMERIC);
		final VarExpr e3 = e().var("E3").ofType(NUMERIC);
		
		final Function lastFunc = FunctionImpl.create(CUBE_SQRT, list(param(B3,NUMERIC)),
												e().sequence(
														e().bindingOf(e3).to(e().invocationOf(CUBE).ofType(NUMERIC).withArgs(b3)),
														e().invocationOf(BuiltInFunction.SQRT).withArgs(e3))
												, NUMERIC);
		final List<Function> expected = cubeExpectedFunctions().snoc(lastFunc);
		assertTrue(listsEqual(result, expected, funcEqPredicate));
		
		
		 
	}
	
	private XSSFWorkbook workbook() 
	{
		return _wb;
	}

}
