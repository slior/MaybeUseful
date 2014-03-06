package ls.tools.excel;

import static com.google.common.base.Preconditions.checkArgument;
import static ls.tools.excel.model.BinaryOp.EQL;
import static ls.tools.excel.model.ExprBuilder.e;
import static ls.tools.excel.model.Functions.createFunction;
import static ls.tools.fj.Util.notEmpty;
import ls.tools.excel.model.BinOpExpr;
import ls.tools.excel.model.Binding;
import ls.tools.excel.model.Expr;
import ls.tools.excel.model.Function;
import ls.tools.excel.model.Param;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import fj.F;
import fj.F2;
import fj.P2;
import fj.data.List;

public final class ExcelTestGenerator
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
		final List<Expr> argValues = funcToTest.parameters().zipWith(inputCells, new F2<Param, String, Expr>() {
			@Override public Expr f(Param p, String inputCell) {
				return e().literal(cellValue(sheetName,inputCell)).ofType(p.type());
			}
		});
		
		final Binding result = e().bindingOf(e().var("result").ofType(funcToTest.returnType()))
							 	  .to(e().invocationOf(funcToTest).withArgs(argValues));
		final BinOpExpr comparison = e().binOp(result.var(), EQL, e().literal(expected).ofType(result.var().type()));
		final List<Param> noParams = List.nil();
		return createFunction("test_" + funcToTest.name() + "_" + expectedOutputCell, noParams, e().sequence(result,comparison), CellType.BOOLEAN);
	}

	public List<Function> generateTestsFor(final Function funcToTest, final String sheetName, final List<List<String>> testCasesInputs, final List<String> expectedOutputs)
	{
		checkArgument(testCasesInputs.length() == expectedOutputs.length(),"Inputs count must match expected output count");
		return testCasesInputs.zip(expectedOutputs)
				.map(new F<P2<List<String>,String>, Function>() {
					@Override public Function f(P2<List<String>, String> testCase) {
						return generateTestFuncFor(funcToTest, sheetName, testCase._1(), testCase._2());
					}
				});
	}
	
	private String cellValue(final String sheetName, final String cellRefFormula)
	{
		final CellReference cr = new CellReference(sheetName + "!" + cellRefFormula);
		return wb.getSheet(sheetName).getRow(cr.getRow()).getCell(cr.getCol()).getRawValue();
	}

	

}
