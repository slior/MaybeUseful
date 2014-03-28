package ls.tools.excel.serialize.js;

import fj.data.List;
import ls.tools.excel.FunctionFormatter;
import ls.tools.excel.model.Function;
import ls.tools.excel.model.Param;

import static com.google.common.base.Preconditions.checkArgument;
import static ls.tools.fj.Util.fj;

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
        final String res = parameters.map(fj(Param::name)).foldLeft(fj((accum,p) -> accum + p + ","),"");
        return res.substring(0,res.length()-1);
	}

	@Override
	public <Func extends Function> String format(final List<Func> functions, final String delimiter)
	{
		checkArgument(functions != null,"Functions can't be null");
		checkArgument(delimiter != null,"Delimiter can't be null");

        final String res = functions.map(fj(f -> format(f))).foldLeft(fj((output,accum)->accum + output + delimiter),"");
        return res.substring(0,res.length());
	}

}
