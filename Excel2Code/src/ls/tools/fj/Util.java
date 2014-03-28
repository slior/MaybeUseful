package ls.tools.fj;

import fj.F2;
import fj.data.List;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

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

	public static boolean notEmpty(final String s ) { return s != null && !"".equals(s); }

    public static <I,R> fj.F<I,R> fj(final Function<I,R> f)
    {
        return new fj.F<I,R>() {
            @Override public R f(I i) {
                return f.apply(i);
            }
        };
    }

    public static <A,B,R> F2<A,B,R> fj(final BiFunction<A,B,R> f)
    {
        return new F2<A,B,R>() {
            @Override public R f(A a, B b) {
                return f.apply(a,b);
            }
        };
    }

/* public static <I> F<I,Boos */

}
