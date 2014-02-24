// $codepro.audit.disable com.instantiations.assist.eclipse.analysis.audit.rule.effectivejava.obeyEqualsContract.obeyGeneralContractOfEquals
package ls.tools.excel;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fj.Show.listShow;
import static fj.Show.showS;
import static java.util.Arrays.deepHashCode;
import static java.util.Objects.hash;
import static ls.tools.fj.Util.genericEqualAndCast;
import static ls.tools.fj.Util.listsEqual;
import static ls.tools.fj.Util.notEmpty;
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
		ParamImpl(final P2<String, CellType> a) { this(a._1(),a._2()); }
		
		ParamImpl(final String name, final CellType type)
		{
			checkArgument(notEmpty(name),"Parameter name can't be empty");
			checkArgument(type != null,"Can't have a null type for parameter");
			this.name = name;
			this.type = type;
		}
		
		@Override public String name() { return this.name;}
		@Override public CellType type() { return this.type; }
		@Override public boolean equals(Object that) // $codepro.audit.disable com.instantiations.assist.eclipse.analysis.audit.rule.effectivejava.obeyEqualsContract.obeyGeneralContractOfEquals
		{
			final P2<Boolean,ParamImpl> gr = genericEqualAndCast(this, that, ParamImpl.class);
			return gr._1() && equal(name(),gr._2().name()) && equal(type(),gr._2().type());
		}
		
		@Override public int hashCode() { return hash(name()) + hash(type()); }
		@Override public String toString() { return name() + " : " + type().toString(); }
		
	}
	
	static Param param(final String name, final CellType type) { return new ParamImpl(name,type); }
	
	private static final class NameTypePairToParam extends F<P2<String, CellType>, Param> { @Override public Param f(P2<String, CellType> a) { return new ParamImpl(a); } }
	static final F<P2<String, CellType>, Param> NAME_TYPE_TO_PARAM = new NameTypePairToParam();
	private static final F<Param,String> PARAM_TO_STRING = new F<Param,String>() {@Override public String f(Param p) { return p.toString(); }};
	

	

	private final String name;
	private final List<Param> params;
	private final Expr body;
	private final CellType type;
	

	static Function create(final String _funcName, final List<Param> params, final Expr _body, final CellType ret) { return new FunctionImpl(_funcName,params, _body, ret); }
	
	
	private FunctionImpl(final String _funcName, final List<Param> _paramList, final Expr _body)
	{
		this(_funcName,_paramList,_body,_body.type());
	}
	
	private FunctionImpl(final String _funcName, final List<Param> _paramList, final Expr _body, final CellType ret)
	{
		this.name = checkNotNull(_funcName);
		
		this.params = checkNotNull(_paramList);
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
				areSameParameters(this.parameters(),f.parameters()) &&
				equal(body(),f.body());
	}

	private boolean areSameParameters(final List<Param> list, final List<Param> list2)
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

	@Override public String toString()
	{
		return name() + ": " + listShow(showS(PARAM_TO_STRING)).showS(parameters()) + " => " + returnType().toString();
	}

}
