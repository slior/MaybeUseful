// $codepro.audit.disable com.instantiations.assist.eclipse.analysis.audit.rule.effectivejava.obeyEqualsContract.obeyGeneralContractOfEquals
package ls.tools.excel.model;

import fj.data.List;
import ls.tools.excel.CellType;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fj.Show.listShow;
import static fj.Show.showS;
import static java.util.Arrays.deepHashCode;
import static java.util.Objects.hash;
import static ls.tools.fj.Util.*;

final class FunctionImpl implements Function
{
	
	private static final class ParamImpl implements Param
	{
		private final String name;
		private final CellType type;

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
            return this == that ||
                   that != null &&
                   that instanceof Param &&
                   equal(name(),((Param)that).name()) && equal(type(),((Param)that).type());
		}
		
		@Override public int hashCode() { return hash(name()) + hash(type()); }
		@Override public String toString() { return name() + " : " + type().toString(); }
		
	}
	
	static Param param(final String name, final CellType type) { return new ParamImpl(name,type); }
	
	private final String name;
	private final List<Param> params;
	private final Expr body;
	private final CellType type;
	

	static Function create(final String _funcName, final List<Param> params, final Expr _body, final CellType ret) { return new FunctionImpl(_funcName,params, _body, ret); }
	
	FunctionImpl(final String _funcName, final List<Param> _paramList, final Expr _body, final CellType ret)
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
        return listsEql(list, list2, nullCheckingEqualPredicate());
    }
	
	@Override public int hashCode() { return hash(name(),body(),returnType()) + deepHashCode(parameters().toArray().array()); }

	@Override public String toString()
	{
        return name() + ": " + listShow(showS(fj(Param::toString))).showS(parameters()) + " => " + returnType().toString();
	}

}
