package ls.tools.excel.model;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static fj.data.List.list;
import static java.util.Arrays.deepHashCode;
import static java.util.Objects.hash;
import static ls.tools.fj.Util.listsEqual;
import ls.tools.excel.CellType;
import ls.tools.excel.Function;
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
	
	public interface VarBuilder { VarExpr ofType(CellType _t); }
		
	/**
	 * Enables <code>create().var("name").ofType("type")</code>
	 */
	public VarBuilder var(final String _n)
	{
		checkArgument(notEmpty(_n),"Variable name can't be empty");
		return new VarBuilder() {

			final String n = _n; 
			
			@Override public VarExpr ofType(final CellType _t)
			{
				checkArgument(_t != null,"Type can't be null");
				return new VarExpr() {

					@Override public CellType type() { return _t; }

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
		LiteralExpr ofType(CellType type);
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

			@Override public LiteralExpr ofType(final CellType type)
			{
				checkArgument(type != null,"type can't be null");
				return new LiteralExpr() {

					@Override public CellType type() { return type; }

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
		BinOpBuilder ofType(CellType type);
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
			CellType type;
			
			@Override
			public BinOpBuilder ofType(final CellType _type)
			{
				checkArgument(_type != null, "type of operator can't be null");
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
					
					@Override public CellType type() { return type; }
					
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
		FunctionInvocationBuilder ofType(CellType t);
		FunctionExpr withArgs(Expr... args );

	}
	
	public FunctionInvocationBuilder invocationOf(final String funcName)
	{
		checkArgument(notEmpty(funcName),"Function name can't be empty");
		return new FunctionInvocationBuilder()
		{
			private CellType type;

			@Override public FunctionExpr withArgs(final Expr... _args)
			{
				return new FunctionExpr()
				{
					final List<Expr> args = List.list(_args);
					@Override public String functionName() { return funcName; }

					@Override public List<Expr> args() { return args; }

					@Override public CellType type() { return type; }

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

			@Override public FunctionInvocationBuilder ofType(final CellType t)
			{
				checkArgument(t != null,"Type can't be null"); //should also check for validity of the type
				type = t;
				return this;
			}
		};
	}
	
	public FunctionInvocationBuilder invocationOf(final Function func)
	{
		return invocationOf(func.name()).ofType(func.returnType());
	}

	public interface BindBuilder
	{
		Binding to(final Expr expr);
	}
	
	public BindBuilder bind(final VarExpr varExpr)
	{
		checkArgument(varExpr != null,"Var can't be null for binding");
		return new BindBuilder()
		{

			@Override public Binding to(final Expr expr)
			{
				checkArgument(expr != null,"expression can't be null for binding");
				return new Binding()
				{
					@Override public VarExpr var() { return varExpr;}
					@Override public Expr expression() { return expr; }
					@Override public CellType type() { return expression().type(); }
				};
			}
			
		};
	}

	public CompositeExpr sequence(final Expr... expressions)
	{
		checkArgument(expressions != null && expressions.length > 0,"Can't have empty expression list for sequence");
		return new CompositeExpr()
		{
			@Override public List<Expr> subExpressions() { return list(expressions); }
			@Override public CellType type() { return subExpressions().last().type(); }
		};
	}

}
