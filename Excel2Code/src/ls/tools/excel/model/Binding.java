package ls.tools.excel.model;

public interface Binding extends Expr
{
	VarExpr var();
	Expr expression();
}
