package ls.tools.excel.serialize.js;

import static org.junit.Assert.assertEquals;
import ls.tools.excel.FormulaConverterTest;
import ls.tools.excel.model.Function;

import org.junit.Before;
import org.junit.Test;

import fj.data.List;

public class JSSerializerTest
{

	private FormulaConverterTest fcTests;
	private JSFormatter formatter;

	@Before
	public void setup()
	{
		fcTests = new FormulaConverterTest();
		formatter = new JSFormatter();
	}
	
	@Test
	public void times2Serialization()
	{
		List<Function> functions = fcTests.simpleScalarMultExpectedResult();
		final String result = formatter.format(functions.head());
		
		final String expected = "function times2(B3) {" +  
								"var _0 = 2;" +  
								"return B3 * _0;" +  
								"}";
		
		System.out.println("Result:\n" + result);
		assertEquals(expected,removeNLs(result));
	}
	
	@Test
	public void cellMultSerialization()
	{
		List<Function> functions = fcTests.simple2CellMultExpectedResult();
		final String result = formatter.format(functions.head());
		
		final String expected = "function mult(C3,B3) {"
								+ "return B3 * C3;"
								+ "}";
		
		System.out.println("Result:\n" + result);
		assertEquals(expected,removeNLs(result));
	}
	
	private String removeNLs(String s) 
	{
		return s.replace(System.getProperty("line.separator"), "");
	}
	
	@Test
	public void testRemoveNLs() throws Exception {
		final String NL = System.getProperty("line.separator");
		final String res = removeNLs("a" + NL + "b" + NL + "c");
		assertEquals("abc", removeNLs(res));
	}
}
