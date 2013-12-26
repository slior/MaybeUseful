package ls.tools.excel;

import static com.google.common.base.Preconditions.checkNotNull;
import ls.tools.excel.model.Expr;


import fj.P2;
import fj.data.List;

final class FunctionImpl implements Function
{

	
	
	private final String name;
	private final List<P2<String, String>> params;
	private final Expr body;
	private final String type;

	FunctionImpl(final String _actionName, final List<P2<String,String>> _paramList, final Expr _body)
	{
		this(_actionName,_paramList,_body,_body.type());
	}
	
	FunctionImpl(final String _actionName, final List<P2<String,String>> _paramList, final Expr _body, final String ret)
	{
		this.name = checkNotNull(_actionName);
		this.params = checkNotNull(_paramList);
		this.body = checkNotNull(_body);
		this.type = checkNotNull(ret);
	}

	@Override
	public String name()
	{
		return name;
	}

	@Override
	public List<P2<String, String>> parameters()
	{
		return params;
	}

	@Override
	public Expr body()
	{
		return body;
	}

	@Override
	public String returnType()
	{
		return type;
	}

}
