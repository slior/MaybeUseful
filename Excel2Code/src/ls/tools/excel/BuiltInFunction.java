package ls.tools.excel;

import static com.google.common.base.Preconditions.checkArgument;
import static fj.data.List.list;
import static ls.tools.excel.CellType.NUMERIC;
import static ls.tools.excel.FunctionImpl.create;
import static ls.tools.excel.FunctionImpl.param;
import static ls.tools.excel.model.ExprBuilder.e;
import ls.tools.excel.model.Expr;
import ls.tools.excel.model.Param;
import fj.F;
import fj.data.List;

public enum BuiltInFunction implements Function
{
	SQRT("SQRT",list(param("X",NUMERIC)),NUMERIC);

	private final Function func;

	private BuiltInFunction(final String _name, final List<Param> _params, final CellType ret)
	{
		this.func = create(_name, _params, e().sequence(), ret);
	}

	@Override public List<Param> parameters() { return func.parameters(); }

	@Override public Expr body() { return func.body(); }

	@Override public CellType returnType() { return func.returnType(); }
	
	/**
	 * Tests whether the given function name is a built in function, as defined in this enumeration.
	 * The test is done by name, under the assumption that no one will name a function in the excel, using the name of a built-in function.
	 * @param functionName The function name to test, can't be empty or null.
	 * @return TRUE iff the given function points to a built in function, as defined in the {@link #BuiltInFunction} enum
	 */
	public static boolean isBuiltinFunction(final String functionName)
	{
		return list(values()).exists(functionWithName(functionName));
	}

	public static BuiltInFunction from(final String functionName)
	{
		return list(values()).find(functionWithName(functionName)).valueE("Can't find built-in function with name: " + functionName);
	}
	
	/**
	 * Create and return a predicate (function from {@link #BuiltInFunction} to boolean) that tests whether a given built in function has the given name. 
	 * @param functionName The name to test with
	 * @return The predicate (function) object.
	 */
	private static final F<BuiltInFunction, Boolean> functionWithName(final String functionName)
	{
		checkArgument(functionName != null && !"".equals(functionName),"Function name can't be null or empty");
		return new F<BuiltInFunction, Boolean>() {
					@Override public Boolean f(BuiltInFunction a) {
						return a.name().equalsIgnoreCase(functionName);
					}
				};
	}
}
