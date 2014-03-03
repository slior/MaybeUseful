package ls.tools.excel.model;

import ls.tools.excel.CellType;
import fj.data.List;

public interface Function
{
	String name();
	List<Param> parameters();
	Expr body();  //function body is currently a single expression
	CellType returnType();
}
