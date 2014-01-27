package ls.tools.excel;

import static fj.data.List.list;
import static ls.tools.excel.CellType.NUMERIC;
import static ls.tools.excel.FunctionImpl.create;
import static ls.tools.excel.FunctionImpl.param;
import static ls.tools.excel.model.ExprBuilder.e;
import ls.tools.excel.model.Expr;
import ls.tools.excel.model.Param;
import fj.data.List;

public enum BuiltInFunction implements Function
{
	SQRT("SQRT",list(param("X",NUMERIC)),NUMERIC);

	
	private final Function func;

	private BuiltInFunction(final String _name, final List<Param> _params, final CellType ret)
	{
		this.func = create(_name, _params, e().sequence(), ret);
	}

	@Override public List<Param> parameters() { return func.parameters(); }

	@Override public Expr body() { return func.body(); }

	@Override public CellType returnType() { return func.returnType(); }
}
