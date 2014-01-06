package ls.tools.excel;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.deepHashCode;
import static java.util.Objects.hash;
import static ls.tools.fj.Util.listsEqual;
import ls.tools.excel.model.Expr;
import ls.tools.excel.model.Param;
import fj.F;
import fj.F2;
import fj.P2;
import fj.data.List;

final class FunctionImpl implements Function
{
	private static final class ParamImpl implements Param
	{
		private final String name;
		private final CellType type;
		ParamImpl(final P2<String, CellType> a) { this.name = a._1(); this.type = a._2(); }
		@Override public String name() { return this.name;}
		@Override public CellType type() { return this.type; }
	}


	private final String name;
	private final List<Param> params;
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
		
		this.params = checkNotNull(_paramList).map(new F<P2<String,CellType>, Param>() { @Override public Param f(P2<String, CellType> a) { return new ParamImpl(a); } });
		this.body = checkNotNull(_body);
		this.type = checkNotNull(ret);
	}

	@Override public String name() { return name; }

	@Override public List<Param> parameters() { return params; }

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

	private boolean sameParameters(final List<Param> list, final List<Param> list2)
	{
		return listsEqual(list, list2, new F2<Param,Param,Boolean>()
		{
			@Override public Boolean f(Param a, Param b)
			{
				if (a == null) return b == null;
				return equal(a.name(),b.name()) && equal(a.type(),b.type());
			}
		});
	}
	
	@Override public int hashCode() { return hash(name(),body(),returnType()) + deepHashCode(parameters().toArray().array()); }


	@Override
	public String toString()
	{
		return name() + ": " + paramsToString() + " => " + returnType().toString();
	}


	private String paramsToString()
	{
		return parameters().foldRight(new F2<Param,String,String>() {
			@Override public String f(Param param, String accum) { return param.name() + " : " + param.type() + "," + accum; }
		}, "");
		
	}

	
	
	
}
