package ls.tools.excel;


import static fj.data.List.list;
import static ls.tools.excel.FormulaConverter.param;
import static ls.tools.excel.model.ExprBuilder.e;
import static org.junit.Assert.assertEquals;

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


import fj.data.List;

public final class FormulaConverterTest 
{

	private static final String MULT_OP = "*";
	private static final String C3 = "C3";
	private static final String B3 = "B3";
	private static final String DECIMAL_FLOAT = "DecimalFloat";
	private static final String CUBE = "cube";
	private static final String SQUARE = "square";
	private static final String TIMES2 = "times2";
	private static final String SHEET1 = "Sheet1";
	private static final String MULT_FUNC_NAME = "mult";
	private XSSFWorkbook _wb;
	private FormulaConverter fc;
	private FunctionFormatter ff;
	
	@Before 
	public void prepareTest() throws InvalidFormatException, FileNotFoundException, IOException
	{
		//!Assumption: it's an openxml format (xlsx, not xls)!
		_wb = (XSSFWorkbook) WorkbookFactory.create(new FileInputStream("test.xlsx"));
		fc = new FormulaConverter();
		ff = new DefaultFormatter();
	}
	
	@Test
	public void simple2CellMultiplication() 
	{
		final String result = fc.rdlFrom(workbook(),SHEET1,MULT_FUNC_NAME);
		@SuppressWarnings("unchecked")
		final String expected = ff.format(new FunctionImpl(MULT_FUNC_NAME, list(param(C3,DECIMAL_FLOAT),param(B3,DECIMAL_FLOAT)), 
															e().binOp(MULT_OP).ofType(DECIMAL_FLOAT).andOperands(
																		e().var(B3).ofType(DECIMAL_FLOAT), 
																		e().var(C3).ofType(DECIMAL_FLOAT)), DECIMAL_FLOAT));
		assertEquals(expected,result);
	}
	
	


	@Test
	public void simpleScalarCellMultiplication() throws Exception
	{
		final String result = fc.rdlFrom(workbook(), SHEET1, TIMES2);
		@SuppressWarnings("unchecked") final String expected = ff.format(new FunctionImpl(TIMES2,list(param(B3,DECIMAL_FLOAT)),
																				e().binOp(MULT_OP).ofType(DECIMAL_FLOAT)
																						.andOperands(
																								e().var(B3).ofType(DECIMAL_FLOAT), 
																								e().literal().withValue("2").ofType(DECIMAL_FLOAT)), DECIMAL_FLOAT));
		assertEquals(expected,result);
	}
	
	@Test
	public void singleParamUsedTwice() throws Exception
	{
		final String result = fc.rdlFrom(workbook(), SHEET1, SQUARE);
		@SuppressWarnings("unchecked") final String expected = ff.format(new FunctionImpl(SQUARE,list(param(B3,DECIMAL_FLOAT)), 
																				e().binOp(MULT_OP).ofType(DECIMAL_FLOAT)
																						.andOperands(
																								e().var(B3).ofType(DECIMAL_FLOAT), 
																								e().var(B3).ofType(DECIMAL_FLOAT)), DECIMAL_FLOAT));
		assertEquals(expected,result);
	}

	@Test
	@Ignore
	public void usingAnotherFormulaAsArgument() throws Exception
	{
		final String result = fc.rdlFrom(workbook(), SHEET1, CUBE);
		final VarExpr b3Var = e().var(B3).ofType(DECIMAL_FLOAT);
		@SuppressWarnings("unchecked")
		final List<FunctionImpl> expectedFunctions = list(new FunctionImpl(SQUARE,list(param(B3,DECIMAL_FLOAT)),
																					e().binOp(MULT_OP).ofType(DECIMAL_FLOAT).andOperands(b3Var, b3Var), DECIMAL_FLOAT),
														  new FunctionImpl(CUBE,list(param(B3,DECIMAL_FLOAT)),
																					e().binOp(MULT_OP).ofType(DECIMAL_FLOAT)
																							.andOperands(
																									e().invocationOf(SQUARE).ofType(DECIMAL_FLOAT).withArgs(b3Var),b3Var), DECIMAL_FLOAT)
																);
		final String expected = ff.format(expectedFunctions,"\n");
		assertEquals(expected,result);
	}
	
	private XSSFWorkbook workbook() 
	{
		return _wb;
	}

}
