package ls.tools.excel.model;

import fj.data.List;
import ls.tools.excel.CellType;

/**
 * A utility class for creation {@link Function} instances
 */
public final class Functions
{
	private Functions() {}
	
	public static Function createFunction(final String _funcName, final List<Param> params, final Expr _body, final CellType ret) 
	{ 
		return FunctionImpl.create(_funcName,params, _body, ret); 
	}
	
	public static Param param(final String name, final CellType type) { return FunctionImpl.param(name,type); }

}
