package ls.tools.excel;

import ls.tools.excel.model.Expr;

import fj.P2;
import fj.data.List;

public interface Function
{
	String name();
	List<P2<String, CellType>> parameters();
	Expr body();  //function body is currently a single expression
	CellType returnType();
}
