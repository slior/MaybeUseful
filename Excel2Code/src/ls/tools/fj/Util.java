package ls.tools.fj;

import fj.data.List;

import java.util.function.BiPredicate;
import java.util.stream.Stream;

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

    public static <T> Stream<T> stream(final List<T> list)
    {
        return list.toCollection().stream(); //TODO: this is probably not very efficient. need to check this
    }

	public static boolean notEmpty(final String s ) { return s != null && !"".equals(s); }
	
}
