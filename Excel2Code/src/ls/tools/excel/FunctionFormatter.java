package ls.tools.excel;

import fj.data.List;

interface FunctionFormatter
{

	String format(final Function f);

	<F extends Function> String format(final List<F> functions, final String delimiter);
}