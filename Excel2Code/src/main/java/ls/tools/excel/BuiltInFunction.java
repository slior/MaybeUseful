package ls.tools.excel;

import fj.F;
import fj.data.List;
import ls.tools.excel.model.Expr;
import ls.tools.excel.model.ExpressionBuilder;
import ls.tools.excel.model.Function;
import ls.tools.excel.model.Param;

import static com.google.common.base.Preconditions.checkArgument;
import static fj.data.List.list;
import static ls.tools.excel.CellType.*;
import static ls.tools.excel.model.Functions.createFunction;
import static ls.tools.excel.model.Functions.param;
import static ls.tools.fj.Util.fj;

public enum BuiltInFunction implements Function, ExpressionBuilder
{
	SQRT("SQRT",list(param("X",NUMERIC)),NUMERIC),
	MOD("MOD",list(param("X",NUMERIC),param("DIVISOR",NUMERIC)),NUMERIC),
	IF("IF",list(param("TEST",BOOLEAN),param("THEN",FORMULA),param("ELSE",FORMULA)),FORMULA);

	private final Function func;

	private BuiltInFunction(final String _name, final List<Param> _params, final CellType ret)
	{
		this.func = createFunction(_name, _params, sequence(), ret);
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
	private static F<BuiltInFunction, Boolean> functionWithName(final String functionName)
	{
		checkArgument(functionName != null && !"".equals(functionName),"Function name can't be null or empty");
        return fj(func -> func.name().equalsIgnoreCase(functionName));
	}
}
