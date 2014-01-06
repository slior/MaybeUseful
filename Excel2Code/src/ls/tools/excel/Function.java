package ls.tools.excel;

import ls.tools.excel.model.Expr;
import ls.tools.excel.model.Param;
import fj.data.List;

public interface Function
{
	String name();
	List<Param> parameters();
	Expr body();  //function body is currently a single expression
	CellType returnType();
}
