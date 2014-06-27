package ls.tools.excel;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class Main 
{

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InvalidFormatException 
	 */
	public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException 
	{
		final Workbook wb = WorkbookFactory.create(new FileInputStream(args[0]));
		final FormulaParsingWorkbook fpwb = XSSFEvaluationWorkbook.create((XSSFWorkbook)wb);
		final Sheet s = wb.getSheetAt(0);
		say("Found " + wb.getNumberOfNames() + " names");
		for (int i = 0; i < wb.getNumberOfNames(); i++)
		{
			final Name n = wb.getNameAt(i);
			say("Found name: " + n.getNameName());
			final CellReference cr = cellRef(n.getRefersToFormula());
			final Cell c = cell(s,cr);
			say("\t at: " + cr.formatAsString() + " with cell type: " + CellType.fromSSCellType(c.getCellType()));
			say("\t formula: " + c.getCellFormula());
			say("\tTokens: ");
			final Ptg[] tokens = FormulaParser.parse(c.getCellFormula(), fpwb, FormulaType.CELL, n.getSheetIndex());
			for (Ptg t : tokens)
			{
				say("\t\t Token: " + t.toString());
				say("\t\t class: " + t.getPtgClass());
				say("\t\t isbase: " + t.isBaseToken());
			}
			
		}

	}


	private static CellReference cellRef(final String cellFormula)
	{
		return new CellReference(cellFormula);
	}
	
	private static Cell cell(final Sheet s, final CellReference cr) 
	{
		final Cell c = s.getRow(cr.getRow()).getCell(cr.getCol());
		return c;
	}

	private static void say(final String m) 
	{
		System.out.println(String.valueOf(m));
		
	}

}
