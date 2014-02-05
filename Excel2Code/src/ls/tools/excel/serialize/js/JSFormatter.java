package ls.tools.excel.serialize.js;

import static com.google.common.base.Preconditions.checkArgument;
import ls.tools.excel.Function;
import ls.tools.excel.FunctionFormatter;
import ls.tools.excel.model.Param;
import fj.F2;
import fj.data.List;

public final class JSFormatter implements FunctionFormatter
{

	
	@Override public String format(final Function f)
	{
		checkArgument(f != null,"Function to format can't be null");
		
		return String.format("%1$s %2$s", header(f), body(f));
		
	}

	private String body(final Function f)
	{
		final JSExpressionSerializer exprSerializer = new JSExpressionSerializer();
		return exprSerializer.serialize(f.body());
	}

	private String header(final Function f)
	{
		return String.format("function %1$s(%2$s)",f.name(),formatParams(f.parameters()));
	}

	private String formatParams(final List<Param> parameters)
	{
//		return listShow(showS(new F<Param,String>() { @Override public String f(Param a) { return a.name(); } }))
//				.showS(parameters);
		final String result = parameters.foldLeft(new F2<String,Param,String>() {
			@Override public String f(String accum, Param p) {
				return accum + p.name() + ",";
			}
		}, "");
		return result.substring(0,result.length()-1);
	}

	@Override
	public <F extends Function> String format(List<F> functions, String delimiter)
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Method format is not implemented yet in JSFormatter");
	}

}
