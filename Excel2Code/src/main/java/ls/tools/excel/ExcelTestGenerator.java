package ls.tools.excel;

import fj.F2;
import fj.data.List;
import ls.tools.excel.model.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static com.google.common.base.Preconditions.checkArgument;
import static ls.tools.excel.model.BinaryOp.EQL;
import static ls.tools.excel.model.Functions.createFunction;
import static ls.tools.fj.Util.fj;
import static ls.tools.fj.Util.notEmpty;

public final class ExcelTestGenerator implements ExpressionBuilder
{

	private final XSSFWorkbook wb;

	ExcelTestGenerator(final XSSFWorkbook workbook)
	{
		checkArgument(workbook != null,"Workbook can't be null for test generator");
		wb = workbook;
	}

	public Function generateTestFuncFor(final Function funcToTest, final String sheetName, final List<String> inputCells, final String expectedOutputCell)
	{
		checkArgument(notEmpty(sheetName),"Sheet name can't be empty");
		checkArgument(inputCells.length() == funcToTest.parameters().length(),"Input cells count must match the function parameter count");
		final String expected = cellValue(sheetName, expectedOutputCell);
		
		//Assumption: the input cells match the parameters in location in the list.
		// i.e. the i-th parameter corresponds to the i-th input cell given.
        final F2<Param,String,Expr> zipFunc = fj(((Param p, String cell) -> literal(cellValue(sheetName, cell)).ofType(p.type())));
        final List<Expr> argValues = funcToTest.parameters().zipWith(inputCells,zipFunc);

		final Binding result = bindingOf(var("result").ofType(funcToTest.returnType()))
							 	  .to(invocationOf(funcToTest).withArgs(argValues));
		final BinOpExpr comparison = binOp(result.var(), EQL, literal(expected).ofType(result.var().type()));
		final List<Param> noParams = List.nil();
		return createFunction("test_" + funcToTest.name() + "_" + expectedOutputCell, noParams, sequence(result,comparison), CellType.BOOLEAN);
	}

	public List<Function> generateTestsFor(final Function funcToTest, final String sheetName, final List<List<String>> testCasesInputs, final List<String> expectedOutputs)
	{
		checkArgument(testCasesInputs.length() == expectedOutputs.length(),"Inputs count must match expected output count");
        return testCasesInputs.zip(expectedOutputs)
                .map(fj(testCase -> generateTestFuncFor(funcToTest, sheetName, testCase._1(), testCase._2())));
	}
	
	private String cellValue(final String sheetName, final String cellRefFormula)
	{
		final CellReference cr = new CellReference(sheetName + "!" + cellRefFormula);
		return wb.getSheet(sheetName).getRow(cr.getRow()).getCell(cr.getCol()).getRawValue();
	}

	

}
