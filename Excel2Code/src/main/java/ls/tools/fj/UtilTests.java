package ls.tools.fj;

import fj.data.List;
import org.junit.Test;

import static ls.tools.fj.Util.listsEql;
import static org.junit.Assert.assertTrue;

public class UtilTests
{

	@Test
	public void twoNilListsShouldBeEqual() throws Exception
	{
		assertTrue(listsEql(List.nil(), List.nil(), (a,b) ->  a == b ));
		
	}
	
	@Test
	public void oneElementListsShouldBeEqual() throws Exception
	{
		assertTrue(listsEql(List.list(1), List.list(1), (a,b) -> a.intValue() == b.intValue()));
	}
	
//	@Test
//	public void genericEqualShouldReturnTrueAndNullForTwoNulls() throws Exception
//	{
//		final P2<Boolean,Object> res = genericEqualAndCast(null, null, Object.class);
//		assertTrue(res._1());
//		assertNull(res._2());
//	}
}
