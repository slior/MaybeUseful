package ls.tools.excel;

import fj.F2;

final class FunctionEqlPredicate extends F2<Function, Function, Boolean>
{
	@Override public Boolean f(Function a, Function b) 
	{
		if (a == null) return b == null;
		final boolean ret = a.equals(b);
		return ret;
	}
}