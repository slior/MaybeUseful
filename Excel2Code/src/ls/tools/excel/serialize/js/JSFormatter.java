package ls.tools.excel.serialize.js;

import static com.google.common.base.Preconditions.checkArgument;
import ls.tools.excel.Function;
import ls.tools.excel.FunctionFormatter;
import ls.tools.excel.model.Param;
import fj.F;
import fj.F2;
import fj.data.List;

public final class JSFormatter implements FunctionFormatter
{
//TODO: need to generate proper calls for built-in functions
	
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
		final String result = parameters.foldLeft(new F2<String,Param,String>() {
			@Override public String f(String accum, Param p) {
				return accum + p.name() + ",";
			}
		}, "");
		return result.substring(0,result.length()-1);
	}

	@Override
	public <Func extends Function> String format(final List<Func> functions, final String delimiter)
	{
		checkArgument(functions != null,"Functions can't be null");
		checkArgument(delimiter != null,"Delimiter can't be null");
		
		final String result = functions
								//format each function
								.map(new F<Func,String>() {
									@Override public String f(Func func) {
										return format(func);
									}
								})
								//join all of them, with the given delimiter
								.foldLeft(new F2<String,String,String>() {
									@Override public String f(String accum, String output) {
										return accum + output + delimiter;
									}}, "");
		return result.substring(0,result.length());
	}

}
