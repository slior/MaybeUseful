package ls.tools.excel;


import static com.google.common.base.Preconditions.checkState;
import static fj.Equal.equal;
import static fj.data.List.nil;
import static ls.tools.excel.CellType.FORMULA;
import static ls.tools.excel.CellType.NUMERIC;
import static ls.tools.excel.CellType.fromSSCellType;
import static ls.tools.excel.model.ExprBuilder.e;

import java.util.Stack;

import ls.tools.excel.model.Expr;

import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.IntPtg;
import org.apache.poi.ss.formula.ptg.MultiplyPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.RefPtg;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


import fj.F;
import fj.P2;
import fj.data.List;
import fj.data.Option;

public final class FormulaConverter
{

	private static final int RESOLVE_NAMES_IN_CONTAINING_SHEET = -1;
	private final Stack<Expr> resultStack = new Stack<>();
	private List<RefPtg> unresolvedSymbols = List.nil();
	private XSSFSheet sheet;
	private FormulaParsingWorkbook fpwb;
	private List<Function> generatedFunctions = nil();
	private final FunctionFormatter formatter = new DefaultFormatter();

	@Deprecated
	String rdlFrom(final XSSFWorkbook wb, final String sheetName, final String name) 
	{
		final Name n = wb.getName(name);
		final CellReference cr = new CellReference(n.getRefersToFormula());
		sheet = wb.getSheet(sheetName);
		fpwb = XSSFEvaluationWorkbook.create(wb);
		final Cell c = sheet.getRow(cr.getRow()).getCell(cr.getCol());
		
		final String formula = c.getCellFormula();
		return formatter.format(convertFormulaToFunction(name, formula));
	}
	
	Function convertFormulaToFunction(final XSSFWorkbook wb, final String sheetName, final String name)
	{
		final Name n = wb.getName(name);
		final CellReference cr = new CellReference(n.getRefersToFormula());
		sheet = wb.getSheet(sheetName);
		fpwb = XSSFEvaluationWorkbook.create(wb);
		final Cell c = sheet.getRow(cr.getRow()).getCell(cr.getCol());
		
		final String formula = c.getCellFormula();
		return convertFormulaToFunction(name, formula);
	}


	private Function convertFormulaToFunction(final String name, final String formula)
	{
		checkState(fpwb != null,"Formula parsing workbook must be resolve for parsing a formula");
		
		final Ptg[] tokens = FormulaParser.parse(formula, fpwb, FormulaType.CELL, RESOLVE_NAMES_IN_CONTAINING_SHEET);

		//tokens are in RPN.
		for (Ptg token : tokens)
		{
			if (token instanceof RefPtg)
				handleReference((RefPtg)token);
			else if (token instanceof MultiplyPtg)
				handleMultOp((MultiplyPtg)token);
			else if (token instanceof IntPtg)
				handleIntLiteral((IntPtg)token);
		}

		final Expr body = resultStack.pop();
		
		return new FunctionImpl(name,paramList(),body,body.type());
	}

	
	
	private void handleIntLiteral(final IntPtg token)
	{
		resultStack.push(e().literal().withValue(token.toFormulaString()).ofType(rdlType(NUMERIC)));
	}


	private Cell cell(final String cellFormula)
	{
		checkState(sheet != null,"Sheet cannot be null");
		final CellReference cr = new CellReference(cellFormula);
		return sheet.getRow(cr.getRow()).getCell(cr.getCol());
	}
	
	private String rdlType(final CellType ct)
	{
		String ret = "";
		switch (ct)
		{
			case NUMERIC : ret = "DecimalFloat"; break;
			case BLANK :
			case STRING : ret = "String"; break;
			case BOOLEAN : ret = "Boolean"; break;
			case FORMULA : 
			case ERROR : 
				throw new IllegalArgumentException("Don't have an rdl type for: " + ct.name());
		}
		return ret;
	}
	
	private List<P2<String, String>> paramList()
	{
		final List<P2<String,String>> ret = unresolvedSymbols
				//Remove duplicates
				.nub(equal(new F<RefPtg, F<RefPtg,Boolean>>() {
					@Override public F<RefPtg, Boolean> f(final RefPtg r1) {
						return new F<RefPtg, Boolean>() {
							@Override public Boolean f(final RefPtg r2) { return r1.toFormulaString().equals(r2.toFormulaString()); }
						};
					}
				})) 
				//Filter out all the formula references
				.filter(new F<RefPtg,Boolean>() {
					@Override public Boolean f(RefPtg a) { return typeOfCellReferencedBy(a) != CellType.FORMULA; }
				})
				//Convert references to param declarations
				.map(new F<RefPtg,P2<String,String>>() {
						@Override public P2<String,String> f(final RefPtg token)
						{
							return param(token.toFormulaString(),rdlType(typeOfCellReferencedBy(token)));
						}});
		return ret;
	}
	
	static final P2<String,String> param(final String name, final String type)
	{
		return new P2<String,String>() 
		{
			@Override public String _1() { return name; }

			@Override public String _2() { return type; }
		};
	}

	private void handleMultOp(final MultiplyPtg token)
	{
		checkState(resultStack.size() >= 2,"Must have at least 2 operands for multiplication");
		final Expr op2 = resultStack.pop();
		final Expr op1 = resultStack.pop();
		resultStack.push(e().binOp("*").ofType(rdlType(NUMERIC)).andOperands(op1,op2));
		
	}

	private void handleReference(final RefPtg token)
	{
		if (typeOfCellReferencedBy(token).equals(FORMULA))
		{
			final Cell c = cell(token.toFormulaString());
			final Option<Name> n = nameForCell(c);
			final String name = n.isSome() ? n.valueE("No name").getNameName() : token.toFormulaString(); //need to make sure the cell formula of the name is a valid RDL identifier
			final Function f = convertFormulaToFunction(name, c.getCellFormula());
			rememberFunction(f);
			//generate the invocation code
//			resultStack.push(e().invocationOf(name).ofType(f.returnType()))
		}
		else
		{
			unresolvedSymbols = unresolvedSymbols.cons(token);
			resultStack.push(e().var(token.toFormulaString()).ofType(rdlType(typeOfCellReferencedBy(token))));
		}
	}

	private void rememberFunction(final Function f)
	{
		generatedFunctions = generatedFunctions .cons(f);
	}
	
	private Option<Name> nameForCell(final Cell c)
	{
		final Workbook wb = sheet.getWorkbook();
		final List<Name> names = namedRangesIn(wb);
		return names.find(new F<Name,Boolean>() {
			@Override public Boolean f(final Name n) { return c.equals(cell(n.getRefersToFormula())); }});
	}
	

	private List<Name> namedRangesIn(final Workbook wb)
	{
		List<Name> ret = List.nil();
		for (int i = wb.getNumberOfNames()-1; i >=0 ; i--)
			ret = ret.cons(wb.getNameAt(i));
		return ret;
	}

	private CellType typeOfCellReferencedBy(final RefPtg ref)
	{
		return fromSSCellType(cell(ref.toFormulaString()).getCellType());
	}

}
