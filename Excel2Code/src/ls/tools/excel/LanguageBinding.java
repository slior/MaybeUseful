package ls.tools.excel;

public interface LanguageBinding
{

	String typeNameFor(final CellType cellType);

	FunctionFormatter formatter();
	
}
