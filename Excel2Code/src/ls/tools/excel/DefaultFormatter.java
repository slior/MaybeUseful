package ls.tools.excel;

import static com.google.common.base.Preconditions.checkArgument;
import ls.tools.excel.model.BinOpExpr;
import ls.tools.excel.model.Expr;
import ls.tools.excel.model.LiteralExpr;
import ls.tools.excel.model.Param;
import ls.tools.excel.model.VarExpr;
import fj.F;
import fj.F2;
import fj.data.List;

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
			return new F<Expr, String>() { @Override public String f(Expr a) { return ((LiteralExpr)a).value(); } };
		}
		else if (expr instanceof VarExpr)
		{
			return new F<Expr,String>() { @Override public String f(Expr a) { return ((VarExpr)a).name(); }};
		}
		else if (expr instanceof BinOpExpr)
		{
			return new F<Expr,String>() {
				@Override public String f(Expr e)
				{
					final BinOpExpr a = (BinOpExpr)e;
					return String.format("%1$s %2$s %3$s",
										formatExpr(a.subExpressions().head()),
										a.op(),
										formatExpr(a.subExpressions().tail().head()));
				}};
		}
		else throw new IllegalArgumentException("Can't format expression of class: " + expr.getClass().getCanonicalName());
		
	}

	private String formatParams(final List<Param> list)
	{
		final String ret = list.foldRight(new F2<Param,String,String>()
		{
			@Override
			public String f(Param param, String accum)
			{
				return param.name() + " : " + param.type() + "," + accum; 
			}
		}, "");
		return ret.substring(0,ret.length()-1);
	}

	@Override
	public <Func extends Function> String format(final List<Func> functions, final String delimiter)
	{
		checkArgument(delimiter != null,"Delimiter can't be null");
		return functions.foldLeft(new F2<String,Func,String>()
		{
			@Override public String f(final String accum, final Func func) {  return accum + delimiter + format(func); }
		}, delimiter);
	}

}
