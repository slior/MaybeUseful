package ls.tools.excel;

import fj.F;
import fj.data.List;
import ls.tools.excel.model.*;

import static com.google.common.base.Preconditions.checkArgument;
import static ls.tools.fj.Util.fj;

final class DefaultFormatter implements FunctionFormatter
{
	@Override
	public String format(final Function func)
	{
		checkArgument(func != null,"Function to be formatted can't be null");
		return String.format("action %1$s(%2$s) { return %3$s; }", func.name(),formatParams(func.parameters()), formatExpr(func.body()));
		
	}
	
	private static String formatExpr(final Expr expr)
	{
		checkArgument(expr != null, "Expression body can't be null");
		return formatFunc(expr).f(expr);
	}

	private static F<Expr, String> formatFunc(final Expr expr)
	{
		if (expr instanceof LiteralExpr)
		{
            return fj(e -> ((LiteralExpr)e).value()); //TODO: will it be better if it's: () -> ((LiteralExpr)expr).value() ?
		}
		else if (expr instanceof VarExpr)
		{
            return fj(e -> ((VarExpr)e).name());
		}
		else if (expr instanceof BinOpExpr)
		{
            return fj(e-> {
                final BinOpExpr binOpExpr = (BinOpExpr)e;
                return String.format("%1$s %2$s %3$s",
                        formatExpr(binOpExpr.subExpressions().head()),
                        binOpExpr.op(),
                        formatExpr(binOpExpr.subExpressions().tail().head()));
            });
		}
		else throw new IllegalArgumentException("Can't format expression of class: " + expr.getClass().getCanonicalName());
		
	}

	private String formatParams(final List<Param> list)
	{
        final String ret = list.foldRight(fj((param, accum) -> param.name() + " : " + param.type() + "," + accum),"");
		return ret.substring(0,ret.length()-1);
	}

	@Override
	public <Func extends Function> String format(final List<Func> functions, final String delimiter)
	{
		checkArgument(delimiter != null,"Delimiter can't be null");
        return functions.foldLeft(fj((accum,func)-> accum + delimiter + format(func)),delimiter);
	}

}
