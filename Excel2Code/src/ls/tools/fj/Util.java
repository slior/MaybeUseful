package ls.tools.fj;

import fj.P2;
import fj.data.List;

import java.util.function.BiPredicate;

import static com.google.common.base.Preconditions.checkArgument;

public final class Util
{

	private Util() {}

    public static <A,B> BiPredicate<A,B> nullCheckingEqualPredicate()
    {
        return (a,b) -> a == null ? b == null : a.equals(b);
    }

    public static <A,B> boolean listsEql(final List<A> list1, List<B> list2, final BiPredicate<A,B> elementsEqlPredicate)
    {
        checkArgument(elementsEqlPredicate != null, "Predicate for comparing elements can't be null");
        if (list1 == null) return list2 == null;
        if (list1.length() != list2.length()) return false;
        if (list1.isEmpty()) return list2.isEmpty();

        final boolean restIsEql = listsEql(list1.tail(),list2.tail(),elementsEqlPredicate);
        return restIsEql && elementsEqlPredicate.test(list1.head(),list2.head());
    }
	
	public static <A,B> P2<A,B> pair(final A a, final B b) {
		return new P2<A,B>() {
			@Override public A _1() { return a; }
			@Override public B _2() { return b; }
		};
	}
	
	@SuppressWarnings("unchecked")
	public static <T> P2<Boolean,T> genericEqualAndCast(Object a, Object b, Class<T> cls)
	{
		if (a == b) return pair(true,(T)b);
		if (b == null) return pair(a == null,null);
		if (!(cls.isAssignableFrom(b.getClass()))) return pair(false,null);
		return pair(true,(T)b);
	}

	public static boolean notEmpty(final String s ) { return s != null && !"".equals(s); }
	
}
