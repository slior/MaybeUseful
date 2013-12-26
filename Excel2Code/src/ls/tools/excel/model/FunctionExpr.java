package ls.tools.excel.model;

import fj.data.List;

public interface FunctionExpr extends Expr
{

	String functionName();
	List<Expr> args();
}
