package ls.tools.excel.model;

public interface BranchExpr extends Expr
{
	Expr test();
	Expr whenTrue();
	Expr whenFalse();
}
