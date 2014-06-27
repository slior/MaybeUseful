package ls.tools.excel.model;

import fj.data.List;
import ls.tools.excel.CellType;

public interface Function
{
	String name();
	List<Param> parameters();
	Expr body();  //function body is currently a single expression
	CellType returnType();
}
