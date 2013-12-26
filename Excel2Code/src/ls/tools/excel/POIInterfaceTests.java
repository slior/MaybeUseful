package ls.tools.excel;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;

public final class POIInterfaceTests 
{

	private static final String SHEET1 = "Sheet1";
	private static final String TEST_FILENAME = "test.xlsx";
	private static final String MULT_NAMED_RANGE = "mult";
	private static final String CUBE_SQRT_NAMED_RANGE = "cube_sqrt";
	private static final String CUBE_NAMED_RANGE = "cube";
	private Workbook wb;
	@Before
	public void setUp() throws Exception {
		wb = WorkbookFactory.create(new FileInputStream(TEST_FILENAME));
	}
	
	@Test
	public void workbookIsXSSFWorkbook() 
	{
		assertTrue(wb instanceof XSSFWorkbook);
	}

	@Test
	public void sheet1IsFoundAtIndex0() throws Exception
	{
		final Sheet s = wb.getSheetAt(0);
		assertEquals(SHEET1,s.getSheetName());
	}
	
	@Test
	public void canRetriveSheetByName() throws Exception
	{
		final Sheet s = wb.getSheet(SHEET1);
		assertEquals(SHEET1, s.getSheetName());
		
	}
	
	@Test
	public void canRetrieveNamedRangesByIndex() throws Exception
	{
		final List<String> firstExpectedNames = new ArrayList<>();
		firstExpectedNames.add(CUBE_NAMED_RANGE);
		firstExpectedNames.add(CUBE_SQRT_NAMED_RANGE);
		firstExpectedNames.add(MULT_NAMED_RANGE);
		for (int i = 0; i < firstExpectedNames.size(); i++)
		{
			final Name n = wb.getNameAt(i);
			assertEquals(firstExpectedNames.get(i), n.getNameName());
		}
	}
	
	@Test
	public void canRetrieveNamedRangeByName() throws Exception
	{
		final Name n = wb.getName(CUBE_NAMED_RANGE);
		assertEquals(CUBE_NAMED_RANGE, n.getNameName());
	}
	
	@Test
	public void namedRangeShouldReferToCellItIsNaming() throws Exception
	{
		final Name n = wb.getName(CUBE_NAMED_RANGE);
		final CellReference cr = new CellReference(n.getRefersToFormula());
		//indices are 0-based. So E3 is actually cell (4,2)
		assertEquals(4, cr.getCol()); 
		assertEquals(2, cr.getRow());
	}
	
	@Test
	public void dataTypeShouldBeRetrievedFromCell() throws Exception
	{
		final CellReference cr = new CellReference("sheet1!$b$3");
		final Sheet s = wb.getSheet(cr.getSheetName());
		final Cell c = s.getRow(cr.getRow()).getCell(cr.getCol());
		assertEquals(Cell.CELL_TYPE_NUMERIC, c.getCellType());
	}

	@Test
	public void resolvingNamedRangeFromCell() throws Exception
	{
		final CellReference cr = new CellReference("sheet1!$b$3");
		final Sheet s = wb.getSheet(cr.getSheetName());
		final Cell c = s.getRow(cr.getRow()).getCell(cr.getCol());
	}
	
	@Test
	public void canRetrieveCellFromSheetAndReference() throws Exception
	{
		final Sheet s = wb.getSheet(SHEET1);
		final Name n = wb.getName(CUBE_NAMED_RANGE);
		final CellReference cr = new CellReference(n.getRefersToFormula());
		final Cell c = s.getRow(cr.getRow()).getCell(cr.getCol());
		assertNotNull(c);
		
	}
	
	@Test
	public void formulaCellsAreEncodedAsSuch() throws Exception
	{
		final Sheet s = wb.getSheet(SHEET1);
		final Name n = wb.getName(CUBE_NAMED_RANGE);
		final CellReference cr = new CellReference(n.getRefersToFormula());
		final Cell c = s.getRow(cr.getRow()).getCell(cr.getCol());
		assertEquals(Cell.CELL_TYPE_FORMULA, c.getCellType());
	}
	
	@Test
	public void FormulaParsingWBCanBeCreatedFromXSSFWorkbook() throws Exception
	{
		final FormulaParsingWorkbook fpwb = XSSFEvaluationWorkbook.create((XSSFWorkbook)wb);
		assertNotNull(fpwb);
	}
	
}

