package ls.tools.excel;

import ls.tools.excel.model.Function;
import fj.data.List;

public interface FunctionFormatter
{

	String format(final Function f);

	<F extends Function> String format(final List<F> functions, final String delimiter);
}
