package ls.tools.excel.serialize.js;

import ls.tools.excel.BuiltInFunction;
import ls.tools.excel.model.*;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static ls.tools.excel.BuiltInFunction.isBuiltinFunction;
import static ls.tools.excel.CellType.STRING;
import static ls.tools.fj.Util.fj;

final class JSExpressionSerializer 
{
	
	private static final String VAR_DECL_PATTERN = "var %1$s = %2$s";
	private static final String BIN_OP_PATTERN = "%1$s %2$s %3$s";
	private static final String FUNCTION_CALL_PATTERN = "%1$s(%2$s)";
	private static final String NL = System.getProperty("line.separator");
	private static final String BLOCK_PATTERN = "{%1$s" + NL + "return %2$s" + NL + "}";
	
	public String serialize(final Expr e)
	{
		checkArgument(e != null,"Expression can't be null");
		if (e instanceof Binding) return serialize((Binding)e);
		else if (e instanceof BinOpExpr) return serialize((BinOpExpr)e); //must be considered before CompositeExpr
		else if (e instanceof FunctionExpr) return serialize((FunctionExpr)e);
		else if (e instanceof LiteralExpr) return serialize((LiteralExpr)e);
		else if (e instanceof VarExpr) return serialize((VarExpr)e);
		else if (e instanceof CompositeExpr) return serialize((CompositeExpr)e);
		else throw new IllegalArgumentException("Can't identify type of expression: " + getClass().getCanonicalName());
	}
	
	private String serialize(Binding e) 
	{
		checkArgument(e != null,"Binding can't be null");
		//String.format is probably not the best way to go performance-wise, but not sure it's critical in this case, and it makes the code clearer.
		return format(VAR_DECL_PATTERN, e.var().name(),serialize(e.expression()));
	}
	
	private String serialize(BinOpExpr e) 
	{
		checkArgument(e != null,"Binary op expression can't be null");
		return format(BIN_OP_PATTERN,
				serialize(e.subExpressions().head()), e.op(), serialize(e.subExpressions().last()));
	}
	private String serialize(FunctionExpr fe) 
	{
		checkArgument(fe != null,"Function expression can't be null");
		return (isBuiltinFunction(fe.functionName())) ? 
				formatBuiltInFunctionCall(fe) : 
				formatUserDefinedFunctionCall(fe);
	}

	private String formatUserDefinedFunctionCall(final FunctionExpr fe) { return format(FUNCTION_CALL_PATTERN,fe.functionName(),formatArgumentsOf(fe)); }

	private String formatBuiltInFunctionCall(final FunctionExpr fe)
	{
        checkArgument(fe != null,"Function expression can't be null");
		final BuiltInFunction f = BuiltInFunction.from(fe.functionName());
		String funcCall ;
		switch (f)
		{
			case SQRT : funcCall = "Math.sqrt"; break;
			default : throw new IllegalStateException("Unrecognized built-in function: " + f.name());
		}
		return format(FUNCTION_CALL_PATTERN,funcCall,formatArgumentsOf(fe));
	}
	
	private String formatArgumentsOf(final FunctionExpr fe)
	{
        checkArgument(fe != null,"Function expression can't be null");
        final String args = fe.args().foldLeft(fj((accum, e) -> accum + serialize(e) + ","), "");
		return args.substring(0, args.length()-1);
	}

	private String serialize(final LiteralExpr e)
	{
		checkArgument(e != null,"Literal expression can't be null");
		final String q = e.type() == STRING ? "'" : "";
		return q + e.value() + q;
	}
	
	private String serialize(final VarExpr e)
	{
		checkArgument(e != null,"Variable expression can't be null");
		return e.name();
	}
	
	private String serialize(final CompositeExpr ce)
	{
		checkArgument(ce != null,"Composite expression can't be null");
        return format(BLOCK_PATTERN,
                        ce.subExpressions().foldLeft(fj((accum, e) -> accum + NL + serialize(e) + ";"),""),
                        serialize(evaluationOf(ce.subExpressions().last())) + ";");
	}

	private Expr evaluationOf(Expr e) //TODO: code smell: this should be encapsulated in the expression definition
	{
		return (e instanceof Binding) ? ((Binding)e).var() : e;
	}
	

}
