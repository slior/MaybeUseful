package ls.tools.excel;


import static com.google.common.base.Objects.equal;
import static fj.data.List.list;
import static junit.framework.Assert.assertTrue;
import static ls.tools.excel.CellType.NUMERIC;
import static ls.tools.excel.FormulaConverter.param;
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
import org.junit.Ignore;
import org.junit.Test;

import fj.F2;
import fj.data.List;

public final class FormulaConverterTest 
{

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
	private final F2<Function, Function, Boolean> funcEqPredicate = new F2<Function, Function, Boolean>() { @Override public Boolean f(Function a, Function b) { return equal(a, b); } };
	
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
		@SuppressWarnings("unchecked")
		final List<Function> expected = list(FunctionImpl.create(MULT_FUNC_NAME, list(param(C3,NUMERIC),param(B3,NUMERIC)), 
				e().binOp(MULT_OP).ofType(NUMERIC).andOperands(
							e().var(B3).ofType(NUMERIC), 
							e().var(C3).ofType(NUMERIC)), NUMERIC));
		assertTrue(listsEqual(result, expected, funcEqPredicate ));
	}
	
	


	@Test
	public void simpleScalarCellMultiplication() throws Exception
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, TIMES2);
		@SuppressWarnings("unchecked") final List<Function> expected = list(FunctionImpl.create(TIMES2,list(param(B3,NUMERIC)),
																				e().binOp(MULT_OP).ofType(NUMERIC)
																						.andOperands(
																								e().var(B3).ofType(NUMERIC), 
																								e().literal().withValue("2").ofType(NUMERIC)), NUMERIC));
		assertTrue(listsEqual(result, expected, funcEqPredicate ));
	}
	
	@Test
	public void singleParamUsedTwice() throws Exception
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, SQUARE);
		@SuppressWarnings("unchecked") final List<Function> expected = list(FunctionImpl.create(SQUARE,list(param(B3,NUMERIC)), 
																				e().binOp(MULT_OP).ofType(NUMERIC)
																						.andOperands(
																								e().var(B3).ofType(NUMERIC), 
																								e().var(B3).ofType(NUMERIC)), NUMERIC));
		assertTrue(listsEqual(result, expected, funcEqPredicate));
	}

	@Test
	public void usingAnotherFormulaAsArgument() throws Exception
	{
		final List<Function> result = fc.formulasFromNamedCell(workbook(), SHEET1, CUBE);
		final VarExpr b3Var = e().var(B3).ofType(NUMERIC);
		@SuppressWarnings("unchecked")
		final List<Function> expectedFunctions = list(FunctionImpl.create(SQUARE,list(param(B3,NUMERIC)),
																					e().binOp(MULT_OP).ofType(NUMERIC).andOperands(b3Var, b3Var), NUMERIC),
														  FunctionImpl.create(CUBE,list(param(B3,NUMERIC)),
																					e().binOp(MULT_OP).ofType(NUMERIC)
																							.andOperands(
																									e().invocationOf(SQUARE).ofType(NUMERIC).withArgs(b3Var),b3Var), NUMERIC)
																);
		assertTrue(listsEqual(result, expectedFunctions, funcEqPredicate));
	}
	
	private XSSFWorkbook workbook() 
	{
		return _wb;
	}

}
