package ls.tools.excel;

import org.apache.poi.ss.formula.ptg.*;
import org.apache.poi.ss.usermodel.Cell;

import static com.google.common.base.Preconditions.checkArgument;

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

	static CellType literalTypeFrom(final ScalarConstantPtg token)
	{
		checkArgument(token != null,"Can't resolve cell type from null token");
		if (token instanceof IntPtg || token instanceof NumberPtg)
			return NUMERIC;
		else if (token instanceof BoolPtg)
			return BOOLEAN;
		else if (token instanceof StringPtg)
			return STRING;
		else throw new IllegalArgumentException("Unrecognized type of scalar token");
	}
} //end of CellTypeEnum