package ls.tools.excel;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.deepHashCode;
import static java.util.Objects.hash;
import static ls.tools.fj.Util.listsEqual;
import ls.tools.excel.model.Expr;
import fj.F2;
import fj.P2;
import fj.data.List;

final class FunctionImpl implements Function
{

	
	
	private final String name;
	private final List<P2<String, CellType>> params;
	private final Expr body;
	private final CellType type;

	static Function create(final String _actionName, final List<P2<String,CellType>> _paramList, final Expr _body, final CellType ret) { return new FunctionImpl(_actionName,_paramList, _body, ret); }
	
	
	private FunctionImpl(final String _actionName, final List<P2<String,CellType>> _paramList, final Expr _body)
	{
		this(_actionName,_paramList,_body,_body.type());
	}
	
	private FunctionImpl(final String _actionName, final List<P2<String,CellType>> _paramList, final Expr _body, final CellType ret)
	{
		this.name = checkNotNull(_actionName);
		this.params = checkNotNull(_paramList);
		this.body = checkNotNull(_body);
		this.type = checkNotNull(ret);
	}

	@Override public String name() { return name; }

	@Override public List<P2<String, CellType>> parameters() { return params; }

	@Override public Expr body() { return body; }

	@Override public CellType returnType() { return type; }

	@Override
	public boolean equals(final Object that)
	{
		if (that == this) return true;
		if (that == null) return false;
		if (!(that instanceof Function)) return false;
		final Function f = (Function)that;
		return 	equal(returnType(), f.returnType()) &&
				equal(name(),f.name()) && 
				sameParameters(this.parameters(),f.parameters()) &&
				equal(body(),f.body());
	}

	private boolean sameParameters(final List<P2<String,CellType>> paramSet1, final List<P2<String,CellType>> paramSet2)
	{
		return listsEqual(paramSet1, paramSet2, new F2<P2<String,CellType>,P2<String,CellType>,Boolean>()
		{
			@Override public Boolean f(P2<String, CellType> a, P2<String, CellType> b)
			{
				if (a == null) return b == null;
				return equal(a._1(),b._1()) && equal(a._2(),b._2());
			}
		});
	}
	
	@Override public int hashCode() { return hash(name(),body(),returnType()) + deepHashCode(parameters().toArray().array()); }

	
	
	
}
