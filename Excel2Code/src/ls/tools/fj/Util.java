package ls.tools.fj;

import static com.google.common.base.Preconditions.checkArgument;
import fj.F2;
import fj.data.List;

public final class Util
{

	private Util() {}

	public static <A,B> boolean listsEqual(final List<A> list1,List<B> list2, final F2<A,B,Boolean> elementsEqualsPredicate)
	{
		checkArgument(elementsEqualsPredicate != null, "Predicate for comparing elements can't be null");
		
		if (list1 == null) return list2 == null;
		if (list1.length() != list2.length()) return false;
		if (list1.isEmpty()) return list2.isEmpty();
		
		return elementsEqualsPredicate.f(list1.head(), list2.head()) && listsEqual(list1.tail(), list2.tail(), elementsEqualsPredicate);
	}
	
	
}
