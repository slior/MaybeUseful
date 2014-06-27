package ls.tools.excel.model;

import ls.tools.excel.CellType;

import static ls.tools.excel.CellType.BOOLEAN;
import static ls.tools.excel.CellType.NUMERIC;

public enum BinaryOp
{

	MULT("*",NUMERIC), EQL("=",BOOLEAN);
	
	
	
	final CellType type;
	final String op;
	
	private BinaryOp(final String _op, final CellType _type)
	{
		this.op = _op;
		this.type = _type;
	}
	
	public CellType type() { return type; }
	public String operator() { return op; }
}
