package ls.tools.excel;

import org.apache.poi.ss.usermodel.Cell;

public enum CellType
{
	NUMERIC(Cell.CELL_TYPE_NUMERIC), 
	STRING(Cell.CELL_TYPE_STRING), 
	FORMULA(Cell.CELL_TYPE_FORMULA), 
	BLANK(Cell.CELL_TYPE_BLANK), 
	BOOLEAN(Cell.CELL_TYPE_BOOLEAN), 
	ERROR(Cell.CELL_TYPE_ERROR);
	
	private final int code;

	CellType(final int _code)
	{
		this.code = _code;
	}
	
	static final CellType fromSSCellType(final int ct)
	{
		for (CellType v : values())
			if (ct == v.code)
				return v;
		throw new IllegalArgumentException("Unrecognized cell type: " + ct);
	}
} //end of CellTypeEnum