package ls.tools.excel;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.FuncPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
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

	private static final int RESOLVE_NAME_IN_CONTAINING_SHEET = -1;
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
		s.getRow(cr.getRow()).getCell(cr.getCol());
	}
	
	@Test
	public void canRetrieveCellFromSheetAndReference() throws Exception
	{
		final Sheet s = wb.getSheet(SHEET1);
		final Name n = wb.getName(CUBE_NAMED_RANGE);
		final Cell c = cellForNameInSheet(s, n);
		assertNotNull(c);
		
	}

	private Cell cellForNameInSheet(final Sheet s, final Name n)
	{
		final CellReference cr = new CellReference(n.getRefersToFormula());
		final Cell c = s.getRow(cr.getRow()).getCell(cr.getCol());
		return c;
	}
	
	@Test
	public void formulaCellsAreEncodedAsSuch() throws Exception
	{
		final Sheet s = wb.getSheet(SHEET1);
		final Name n = wb.getName(CUBE_NAMED_RANGE);
		final Cell c = cellForNameInSheet(s, n);
		assertEquals(Cell.CELL_TYPE_FORMULA, c.getCellType());
	}
	
	@Test
	public void FormulaParsingWBCanBeCreatedFromXSSFWorkbook() throws Exception
	{
		final FormulaParsingWorkbook fpwb = XSSFEvaluationWorkbook.create((XSSFWorkbook)wb);
		assertNotNull(fpwb);
	}
	
	
	@Test
	public void identifyingBuiltInFunctionsByPtgClass() throws Exception
	{
		final Cell c = cellForNameInSheet(wb.getSheet(SHEET1), wb.getName(CUBE_SQRT_NAMED_RANGE));
		final FormulaParsingWorkbook fpwb = XSSFEvaluationWorkbook.create((XSSFWorkbook)wb);
		final Ptg[] tokens = FormulaParser.parse(c.getCellFormula(), fpwb, FormulaType.CELL, RESOLVE_NAME_IN_CONTAINING_SHEET);
		
		final Ptg funcToken = tokens[1];
		assertTrue(funcToken instanceof FuncPtg); //is built in function
		final FuncPtg ftok = (FuncPtg)funcToken;
		assertEquals("SQRT",ftok.getName());
		
		
	}
}

