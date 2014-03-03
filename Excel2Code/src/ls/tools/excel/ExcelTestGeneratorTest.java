package ls.tools.excel;

import static fj.data.List.list;
import static fj.data.List.nil;
import static ls.tools.excel.CellType.BOOLEAN;
import static ls.tools.excel.CellType.NUMERIC;
import static ls.tools.excel.model.BinaryOp.EQL;
import static ls.tools.excel.model.ExprBuilder.e;
import static ls.tools.excel.model.Functions.createFunction;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import ls.tools.excel.model.Binding;
import ls.tools.excel.model.Function;
import ls.tools.excel.model.Param;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;

import fj.data.List;

public class ExcelTestGeneratorTest
{

	private static final String H5 = "H5";
	private static final String SHEET1 = "Sheet1";
	private static final String EXPECTED_RESULT_VAR_NAME = "result";
	private static final FunctionEqlPredicate functionsEqual = new FunctionEqlPredicate();
	private static final List<Param> NO_PARAMS = nil();
	
	private XSSFWorkbook _wb;
	private ExcelTestGenerator testGenerator;
	
	@Before 
	public void prepareTest() throws InvalidFormatException, FileNotFoundException, IOException
	{
		//!Assumption: it's an openxml format (xlsx, not xls)!
		_wb = (XSSFWorkbook) WorkbookFactory.create(new FileInputStream("test.xlsx"));
		testGenerator = new ExcelTestGenerator(workbook());
	}
	
	private XSSFWorkbook workbook() { return _wb; }
	
	@Test
	public void creatingSingleTestForTimes2()
	{
		final Function scalarMultFunc = new FormulaConverterTest().simpleScalarMultExpectedResult().head();
		final Function testFunc = testGenerator.generateTestFuncFor(scalarMultFunc,SHEET1,list("B5"),H5);
		
		final Binding resultVarDef = e().bindingOf(e().var(EXPECTED_RESULT_VAR_NAME).ofType(scalarMultFunc.returnType()))
				   				  		.to(e().invocationOf(scalarMultFunc).withArgs(e().literal("3").ofType(NUMERIC)));
		final Function expected = createFunction("test_" + scalarMultFunc.name() + "_" + H5, NO_PARAMS , 
														e().sequence(
																resultVarDef,
//																e().binOp("=").andOperands(resultVarDef.var(), e().literal("6").ofType(NUMERIC))),
																e().binOp(resultVarDef.var(), EQL, e().literal("6").ofType(NUMERIC))),
														BOOLEAN);
		
		assertTrue("Simple scalar multiplication test creation failed for B5 input and H5 output", 
					functionsEqual.f(testFunc, expected));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void nonMatchingInputCellsAndParameterCountThrowsAnException()
	{
		final Function scalarMultFunc = new FormulaConverterTest().simpleScalarMultExpectedResult().head();
		testGenerator.generateTestFuncFor(scalarMultFunc, SHEET1, list("",""), H5);
	}
	
//	@Test
//	public void generateTestsForSeveralFunctions()
//	{
//		final FormulaConverterTest formulaConverterTest = new FormulaConverterTest();
//		final Function scalarMultFunc = formulaConverterTest.simpleScalarMultExpectedResult().head();
////		final Function cellMultFunc = formulaConverterTest. 
//	}

}
