package ls.tools.excel.model;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static fj.data.List.list;
import static java.util.Arrays.deepHashCode;
import static java.util.Objects.hash;
import static ls.tools.fj.Util.listsEqual;
import fj.F2;
import fj.data.List;

public final class ExprBuilder
{

	private static class ExprEqualsPredicate extends F2<Expr,Expr,Boolean>
	{

		@Override
		public Boolean f(final Expr a, final Expr b)
		{
			if (a == null) return b == null;
			return a.equals(b);
		}
		
	}
	
	private final ExprEqualsPredicate exprEqlPredicate = new ExprEqualsPredicate();
	
	private ExprBuilder() {
		
	};
	
	private static final ExprBuilder mainBuilderInstance = new ExprBuilder(); 
	public static ExprBuilder e() { return mainBuilderInstance; } //we can it this way since it doesn't hold any state
	
	private static boolean notEmpty(final String s ) { return s != null && !"".equals(s); }
	
	public interface VarBuilder { VarExpr ofType(String _t); }
		
	/**
	 * Enables <code>create().var("name").ofType("type")</code>
	 */
	public VarBuilder var(final String _n)
	{
		checkArgument(notEmpty(_n),"Variable name can't be empty");
		return new VarBuilder() {

			final String n = _n; 
			
			@Override public VarExpr ofType(final String _t)
			{
				checkArgument(notEmpty(_t),"Type can't be empty");
				return new VarExpr() {

					@Override public String type() { return _t; }

					@Override public String name() { return n; }

					@Override
					public boolean equals(final Object that)
					{
						if (this == that) return true;
						if (that == null) return false;
						if (!(that instanceof VarExpr)) return false;
						final VarExpr ve = (VarExpr)that;
						return equal(type(), ve.type()) && equal(name(),ve.name());
					}

					@Override
					public int hashCode() { return hash(this.name(),this.type()); }


				};
					
					
			}

		};
	}
	
	//---- 
	
	public interface LiteralBuilder
	{
		LiteralBuilder withValue(String val);
		LiteralExpr ofType(String typeName);
	}
	
	/**
	 * Enables <code>create().literal().withValue("val").ofType("type");</code>
	 */
	public LiteralBuilder literal()
	{
		return new LiteralBuilder() {
			String v;
			@Override public LiteralBuilder withValue(final String val) 
			{
				checkArgument(notEmpty(val),"Value can't be empty");
				v = val;
				return this;

			}

			@Override public LiteralExpr ofType(final String typeName)
			{
				checkArgument(notEmpty(typeName),"type name can't be null");
				return new LiteralExpr() {

					@Override public String type() { return typeName; }

					@Override
					public String value() { return v; }

					@Override
					public boolean equals(Object that)
					{
						if (this == that) return true;
						if (that == null) return false;
						if (!(that instanceof LiteralExpr)) return false;
						final LiteralExpr le = (LiteralExpr)that;
						return equal(type(),le.type()) && equal(value(),le.value());
					}

					@Override public int hashCode() { return hash(type(),value()); }
					
					
				};
			}
		};
	}
	//----
	
	public interface BinOpBuilder
	{
		BinOpBuilder ofType(String type);
		BinOpExpr andOperands(Expr e1,Expr e2);
	}

	
	/**
	 * Enables <code>create().binOp().withOperator(op).ofType("type").andOperands(expr1,expr2)</code> 
	 */
	public BinOpBuilder binOp(final String _op)
	{
		checkArgument(notEmpty(_op),"Operand can't be legal"); //should probably also check validity of the operator
		return new BinOpBuilder()
		{
			final String op = _op;
			String type;
			
			@Override
			public BinOpBuilder ofType(final String _type)
			{
				checkArgument(notEmpty(_type), "type of operator can't be null");
				type = _type;
				return this;
			}
			
			@Override
			public BinOpExpr andOperands(final Expr e1, final Expr e2)
			{
				checkArgument(e1 != null,"Expression #1 can't be null");
				checkArgument(e2 != null,"Expression #2 can't be null");
				return new BinOpExpr()
				{
					
					@Override public String type() { return type; }
					
					@Override public List<Expr> subExpressions() { return list(e1,e2); }
					
					@Override public String op() { return op; }

					@Override
					public boolean equals(final Object that)
					{
						if (this == that) return true;
						if (that == null) return false;
						if (!(that instanceof BinOpExpr)) return false;
						final BinOpExpr boe = (BinOpExpr)that;
						return 	equal(type(),boe.type()) &&
								equal(op(),boe.op()) &&
								listsEqual(this.subExpressions(),boe.subExpressions(),exprEqlPredicate);
					}

					@Override
					public int hashCode() { return hash(type(),op()) + deepHashCode(subExpressions().toArray().array()); }
					
					
				};
			}
		};
	}

	//----
	
	public interface FunctionInvocationBuilder
	{
		FunctionInvocationBuilder ofType(String t);
		FunctionExpr withArgs(Expr... args );

	}
	
	public FunctionInvocationBuilder invocationOf(final String funcName)
	{
		checkArgument(notEmpty(funcName),"Function name can't be empty");
		return new FunctionInvocationBuilder()
		{
			private String type;

			@Override public FunctionExpr withArgs(final Expr... _args)
			{
				return new FunctionExpr()
				{
					final List<Expr> args = List.list(_args);
					@Override public String functionName() { return funcName; }

					@Override public List<Expr> args() { return args; }

					@Override public String type() { return type; }

					@Override
					public boolean equals(Object that)
					{
						if (this == that) return true;
						if (that == null) return false;
						if (!(that instanceof FunctionExpr)) return false;
						final FunctionExpr fe = (FunctionExpr)that;
						return equal(functionName(),fe.functionName()) && equal(type(),fe.type()) && listsEqual(args(), fe.args(), exprEqlPredicate);
					}

					@Override public int hashCode() { return hash(functionName(),type()) + deepHashCode(args().toArray().array()); }
					
					
					
				};
			}

			@Override public FunctionInvocationBuilder ofType(final String t)
			{
				checkArgument(notEmpty(t),"Type can't be empty"); //should also check for validity of the type
				type = t;
				return this;
			}
		};
	}

}
