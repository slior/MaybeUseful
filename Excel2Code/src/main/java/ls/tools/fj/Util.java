package ls.tools.fj;

import fj.F2;
import fj.data.List;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.valueOf;

public final class Util
{

	private Util() {}

    private static final String NL = System.getProperty("line.separator");

    public static <A,B> BiPredicate<A,B> nullCheckingEqualPredicate()
    {
        return (a,b) -> a == null ? b == null : a.equals(b);
    }

    /**
     * Using #listsEql without throwing an exception.
     * @see #listsEql(fj.data.List, fj.data.List, java.util.function.BiPredicate)
     */
    public static <A,B> boolean listsEql(final List<A> list1, List<B> list2, final BiPredicate<A,B> elementsEqlPredicate)
    {
        return listsEql(list1,list2,elementsEqlPredicate,false);
    }

    /**
     * Tests whether the two given lists are equal, element-by-element, according to the given binary predicate.
     * It returns true iff both lists are of the same length, and the given predicate returns true for each corresponding element pair.
     *
     * If one list is null, the other one must be null as well.
     *
     * There is also the option, for debugging purposes, to ask for an exception to be thrown when a non-equal element pair is met.
     * @param list1 The first list to test
     * @param list2 The 2nd list to test
     * @param elementsEqlPredicate The predicate used to test elements
     * @param exceptionOnNonEql Whether or not to throw an exception in case a comparison fails
     * @param <A> The type of the elements in the 1st list
     * @param <B> The type of the elements in the 2nd list
     * @return whether or not all elements are equal, based on the given predicate
     */
    public static <A,B> boolean listsEql(final List<A> list1, List<B> list2, final BiPredicate<A,B> elementsEqlPredicate, final boolean exceptionOnNonEql)
    {
        checkArgument(elementsEqlPredicate != null, "Predicate for comparing elements can't be null");
        if (list1 == null) return orThrow(list2 == null,exceptionOnNonEql,"list1 is null but list2 isn't");
        if (list2 == null) return orThrow(false,exceptionOnNonEql,"list2 is null but list1 isn't");
        if (list1.length() != list2.length()) return orThrow(false,exceptionOnNonEql,"lengths are not equal");
        if (list1.isEmpty()) return orThrow(list2.isEmpty(),exceptionOnNonEql,"list1 is empty but list2 isn't");
        if (list2.isEmpty()) return orThrow(false,exceptionOnNonEql,"list2 is empty but list1 isn't");

        final boolean restIsEql = listsEql(list1.tail(),list2.tail(),elementsEqlPredicate,exceptionOnNonEql);
        final boolean headIsEql = elementsEqlPredicate.test(list1.head(),list2.head());
        if (exceptionOnNonEql && !headIsEql)
            throw new RuntimeException(NL + valueOf(list1.head()) + NL + "\tand" + NL + valueOf(list2.head()) + NL + "\tare not equal");
        return restIsEql && headIsEql;
    }

    /**
     * Return the given boolean value or throw an exception with the given message in case the boolean return value is false.
     * @see #listsEql(fj.data.List, fj.data.List, java.util.function.BiPredicate, boolean)
     * @param ret The return value
     * @param throwException Whether or not to throw an exception in case ret is false;
     * @param msg The exception message
     * @return The return value given, or an exception in case the return value is false, and an exception is requested.
     */
    private static boolean orThrow(final boolean ret, final boolean throwException, final String msg)
    {
        // One could consider using a threadLocal to reduce some code here - remove the 2nd parameter
        if (!ret && throwException)
            throw new RuntimeException(msg);
        else return ret;
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


}
