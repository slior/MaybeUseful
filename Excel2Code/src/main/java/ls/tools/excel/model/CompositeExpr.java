package ls.tools.excel.model;

import fj.data.List;

public interface CompositeExpr extends Expr
{
	List<Expr> subExpressions();
}
