package ls.tools.fj;

import static ls.tools.fj.Util.listsEqual;
import static org.junit.Assert.*;

import org.junit.Test;

import fj.F2;
import fj.data.List;

public class UtilTests
{

	@Test
	public void twoNilListsShouldBeEqual() throws Exception
	{
		assertTrue(listsEqual(List.nil(), List.nil(), new F2<Object,Object,Boolean>() {

			@Override
			public Boolean f(Object a, Object b)
			{
				return a == b;
			}}));
		
	}
	
	@Test
	public void oneElementListsShouldBeEqual() throws Exception
	{
		assertTrue(listsEqual(List.list(1), List.list(1), new F2<Integer,Integer,Boolean>() {

			@Override
			public Boolean f(Integer a, Integer b)
			{
				return a.intValue() == b.intValue();
			}}));
	}
}
