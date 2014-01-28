package ls.tools.fj;

import static com.google.common.base.Preconditions.checkArgument;
import static fj.data.List.list;
import fj.F;
import fj.data.List;
import fj.data.Option;

/**
 * Dispatch a function according to the runtime type of the given argument.
 * For now, doesn't look up the hierarchy, just tries to find the concrete type, and dispath to that.
 */
public final class FunctionDispatcher<T,R>
{

	private List<F<? extends T, R>> handlers;

	public FunctionDispatcher(final F<? extends T,R>... funcs)
	{
		checkArgument(funcs != null);
		handlers = list(funcs);
	}
	
	public R f(final T arg)
	{
		final F<F<? extends T, R>,Boolean> findFunc = new F<F<? extends T, R>,Boolean>() {

			@Override
			public Boolean f(F<? extends T, R> handler)
			{
				final Class<?> firstTypeClass = handler.getClass().getTypeParameters()[0].getGenericDeclaration();
				return firstTypeClass.isInstance(arg);
			}};
		final Option<F<? extends T, R>> maybeFunc = handlers.find(findFunc);
		return maybeFunc.valueE("Couldn't find function to match: " + arg.getClass()).f(arg);
	}
	
	
}
