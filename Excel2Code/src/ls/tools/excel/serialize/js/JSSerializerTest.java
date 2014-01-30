package ls.tools.excel.serialize.js;

import static org.junit.Assert.*;
import ls.tools.excel.FormulaConverterTest;
import ls.tools.excel.Function;

import org.junit.Before;
import org.junit.Test;

import fj.data.List;

public class JSSerializerTest
{

	private FormulaConverterTest fcTests;
	private JSFormatter formatter;

	@Test
	public void times2Serialization()
	{
		List<Function> functions = fcTests.simpleScalarMultExpectedResult();
		final String result = formatter.format(functions.head());
		
		System.out.println("Result:\n" + result);
		
		
	}
	
	@Before
	public void setup()
	{
		fcTests = new FormulaConverterTest();
		formatter = new JSFormatter();
	}
	
	@Test
	public void cellMultSerialization()
	{
		List<Function> functions = fcTests.simple2CellMultExpectedResult();
		final String result = formatter.format(functions.head());
		
		System.out.println("Result:\n" + result);
	}
}
