package ls.tools.excel;

import fj.P2;
import fj.data.List;
import ls.tools.excel.model.*;
import ls.tools.fj.Util;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.BiPredicate;

import static fj.data.List.list;
import static fj.data.List.nil;
import static ls.tools.excel.CellType.BOOLEAN;
import static ls.tools.excel.model.BinaryOp.EQL;
import static ls.tools.excel.model.Functions.createFunction;
import static ls.tools.fj.Util.listsEql;
import static ls.tools.fj.Util.nullCheckingEqualPredicate;
import static org.junit.Assert.assertTrue;

public final class ExcelTestGeneratorTest implements ExpressionBuilder
{

	private static final String B5 = "B5";
	private static final String H5 = "H5";
	private static final String SHEET1 = "Sheet1";
	private static final String EXPECTED_RESULT_VAR_NAME = "result";
	private static final BiPredicate<Function,Function> functionsEqual = nullCheckingEqualPredicate();
	private static final List<Param> NO_PARAMS = nil();
	
	private XSSFWorkbook _wb;
	private ExcelTestGenerator testGenerator;
	
	@Before 
	public void prepareTest() throws InvalidFormatException, IOException
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
		
		final Function expected = generateExpectedTestFunctionFor(scalarMultFunc, list(numericLiteral(3)), numericLiteral(6), H5);
		
		assertTrue("Simple scalar multiplication test creation failed for B5 input and H5 output", 
					functionsEqual.test(testFunc, expected));
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
														list(numericLiteral(1)),
														list(numericLiteral(3)));
		final List<LiteralExpr> expectedOutputs = list(
														numericLiteral(2),
														numericLiteral(6));
		
		final List<P2<P2<List<LiteralExpr>, LiteralExpr>, String>> testCases = inputValues.zip(expectedOutputs).zip(expectedOutputCells);
        final List<Function> expectedFunctions = testCases
                                                    .map(Util.fj(testCase -> generateExpectedTestFunctionFor(scalarMultFunc,
                                                            testCase._1()._1(), //inputs
                                                            testCase._1()._2(), //expected result
                                                            testCase._2())));   //expected result cell

		assertTrue(listsEql(testFunctions, expectedFunctions, functionsEqual));
	}
	
	private Function generateExpectedTestFunctionFor(final Function functionTested,final List<LiteralExpr> inputs, final LiteralExpr expectedResult, final String expectedResultCell)
	{
		final Binding resultVarDef = bindingOf(var(EXPECTED_RESULT_VAR_NAME).ofType(functionTested.returnType()))
			  		.to(invocationOf(functionTested).withArgs(inputs));
		return createFunction("test_" + functionTested.name() + "_" + expectedResultCell, NO_PARAMS , 
								sequence(resultVarDef, binOp(resultVarDef.var(), EQL, expectedResult)),
								BOOLEAN);
	}
}
