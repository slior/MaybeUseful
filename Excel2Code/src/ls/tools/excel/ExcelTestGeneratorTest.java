package ls.tools.excel;

import static fj.data.List.list;
import static fj.data.List.nil;
import static ls.tools.excel.CellType.BOOLEAN;
import static ls.tools.excel.CellType.NUMERIC;
import static ls.tools.excel.model.BinaryOp.EQL;
import static ls.tools.excel.model.ExprBuilder.e;
import static ls.tools.excel.model.Functions.createFunction;
import static ls.tools.fj.Util.listsEqual;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import ls.tools.excel.model.Binding;
import ls.tools.excel.model.Function;
import ls.tools.excel.model.LiteralExpr;
import ls.tools.excel.model.Param;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;

import fj.F;
import fj.P2;
import fj.data.List;

public class ExcelTestGeneratorTest
{

	private static final String B5 = "B5";
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
		final Function testFunc = testGenerator.generateTestFuncFor(scalarMultFunc,SHEET1,list(B5),H5);
		
		final Function expected = generateExpectedTestFunctionFor(scalarMultFunc, list(e().numericLiteral(3)), e().numericLiteral(6), H5);
		
		assertTrue("Simple scalar multiplication test creation failed for B5 input and H5 output", 
					functionsEqual.f(testFunc, expected));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void nonMatchingInputCellsAndParameterCountThrowsAnException()
	{
		final Function scalarMultFunc = new FormulaConverterTest().simpleScalarMultExpectedResult().head();
		testGenerator.generateTestFuncFor(scalarMultFunc, SHEET1, list("",""), H5);
	}
	
	@Test
	public void generateSeveralTests()
	{
		final Function scalarMultFunc = new FormulaConverterTest().simpleScalarMultExpectedResult().head();
		@SuppressWarnings("unchecked")
		final List<List<String>> inputs = list(list("B3"),list(B5));
		final List<String> expectedOutputCells = list("H3",H5);
		final List<Function> testFunctions = testGenerator.generateTestsFor(scalarMultFunc,SHEET1,inputs,expectedOutputCells );
		
		@SuppressWarnings("unchecked")
		final List<List<LiteralExpr>> inputValues = list(
														list(e().numericLiteral(1)),
														list(e().numericLiteral(3)));
		final List<LiteralExpr> expectedOutputs = list(
														e().numericLiteral(2),
														e().numericLiteral(6));
		
		final List<P2<P2<List<LiteralExpr>, LiteralExpr>, String>> testCases = inputValues.zip(expectedOutputs).zip(expectedOutputCells);
		final List<Function> expectedFunctions = testCases.map(new F<P2<P2<List<LiteralExpr>,LiteralExpr>,String>, Function>() {
			@Override public Function f(P2<P2<List<LiteralExpr>, LiteralExpr>, String> testCase) {
				return generateExpectedTestFunctionFor(scalarMultFunc, testCase._1()._1(), testCase._1()._2(), testCase._2());
			}
		});
		
		assertTrue(listsEqual(testFunctions, expectedFunctions, functionsEqual));
	}
	
	private Function generateExpectedTestFunctionFor(final Function functionTested,final List<LiteralExpr> inputs, final LiteralExpr expectedResult, final String expectedResultCell)
	{
		final Binding resultVarDef = e().bindingOf(e().var(EXPECTED_RESULT_VAR_NAME).ofType(functionTested.returnType()))
			  		.to(e().invocationOf(functionTested).withArgs(inputs));
		return createFunction("test_" + functionTested.name() + "_" + expectedResultCell, NO_PARAMS , 
								e().sequence(resultVarDef, e().binOp(resultVarDef.var(), EQL, expectedResult)),
								BOOLEAN);
	}
}
