package ls.tools.excel.model;

import fj.data.List;
import ls.tools.excel.CellType;

import java.util.function.BiPredicate;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static fj.data.List.list;
import static java.lang.String.format;
import static java.util.Arrays.deepHashCode;
import static java.util.Objects.hash;
import static ls.tools.excel.CellType.BOOLEAN;
import static ls.tools.fj.Util.*;

public final class ExprBuilder
{

	private static final BiPredicate<Expr,Expr> exprEqlPredicate = nullCheckingEqualPredicate();
	private ExprBuilder() { }
	
	private static final ExprBuilder mainBuilderInstance = new ExprBuilder(); 
	public static ExprBuilder e() { return mainBuilderInstance; } //we can do it this way since it doesn't hold any state
	
	public interface VarBuilder { VarExpr ofType(CellType _t); }
		
	/**
	 * Enables <code>e().var("name").ofType("type")</code>
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
					@Override public boolean equals(final Object that)
					{
                        return this == that ||
                                that != null &&
                                that instanceof VarExpr &&
                                equal(type(),((VarExpr)that).type()) && equal(name(),((VarExpr)that).name());
					}

					@Override public int hashCode() { return hash(this.name(),this.type()); }
					@Override public String toString() { return name() + " : " + type().toString(); }
				};
			}
		};
	}
	
	//---- 
	
	public interface LiteralBuilder
	{
		LiteralExpr ofType(CellType type);
	}
	
	private static class LiteralExprImpl implements LiteralExpr
	{
		private final String val;
		private final CellType type;

		LiteralExprImpl(final String _val, final CellType _type) 
		{ 
			checkArgument(_val != null,"Literal value can't be null");
			
			this.val = _val; 
			this.type = _type;
		}
		
		LiteralExprImpl(final boolean _val) { this(String.valueOf(_val),CellType.BOOLEAN); }
		LiteralExprImpl(final Number _val) { this(String.valueOf(_val),CellType.NUMERIC); }

		@Override public CellType type() { return type; }
		@Override public String value() { return val; }
		
		@Override
		public boolean equals(Object that)
		{
			if (this == that) return true;
			if (that == null) return false;
			if (!(that instanceof LiteralExpr)) return false;
			final LiteralExpr le = (LiteralExpr)that;
			final boolean sameType = equal(type(),le.type());
			return sameType && (type().equals(BOOLEAN)) ?
					equal(value().toLowerCase(),le.value().toLowerCase()) : 
					equal(value(),le.value());
		}

		@Override public int hashCode() { return hash(type(),value()); }
		@Override public String toString() { return value(); }
	} //end of LiteralExprImpl class
	/**
	 * Enables <code>e().literal("val").ofType("type");</code>
	 */
	public LiteralBuilder literal(final String val)
	{
        return (type) -> new LiteralExprImpl(val,type);
	}
	
	/**
	 * A syntactic sugar for <code>literal("num").ofType(NUMERIC)</code>
	 * @param num The number for the literal
	 * @return A new {@link LiteralExpr}.
	 * @see #literal(String)
	 */
	public LiteralExpr numericLiteral(final Number num) { return new LiteralExprImpl(num); }
	/**
	 * A syntactic sugar for <code>literal("boolean value").ofType(BOOLEAN)</code>
	 * @param b The boolean value for the literal
	 * @return A new {@link LiteralExpr}
	 * @see #literal(String)
	 */
	public LiteralExpr booleanLiteral(final boolean b) { return new LiteralExprImpl(b); }
	//----
	
	public BinOpExpr binOp(final Expr e1, final BinaryOp op, final Expr e2)
	{
		return new BinOpExpr()
		{
			
			@Override public CellType type() { return op.type(); }
			@Override public List<Expr> subExpressions() { return list(e1,e2); }
			@Override public String op() { return op.operator(); }
			@Override public boolean equals(final Object that)
			{
				if (this == that) return true;
				if (that == null) return false;
				if (!(that instanceof BinOpExpr)) return false;
				final BinOpExpr boe = (BinOpExpr)that;
				return 	equal(type(),boe.type()) &&
						equal(op(),boe.op()) &&
                        listsEql(this.subExpressions(),boe.subExpressions(),exprEqlPredicate);
			}

			@Override public int hashCode() { return hash(type(),op()) + deepHashCode(subExpressions().toArray().array()); }
			@Override public String toString() { return "(" + e1.toString() + ") " + op() + " (" + e2.toString() + ")"; }
			
			
		};
	}
	//----
	
	public interface FunctionInvocationBuilder
	{
		FunctionExpr withArgs(Expr... args );
		FunctionExpr withArgs(List<? extends Expr> args);
	}
	
	
	public FunctionInvocationBuilder invocationOf(final Function func)
	{
		checkArgument(func != null,"Function can't be null");
		return new FunctionInvocationBuilder()
		{
			final class FunctionExprImpl implements FunctionExpr
			{
				final List<Expr> args;
				@SuppressWarnings("unchecked")
				FunctionExprImpl(final List<? extends Expr> _args) { this.args = (List<Expr>) (_args == null ? List.nil() : _args); }
				
				@Override public String functionName() { return func.name(); }
				@Override public List<Expr> args() { return args; }
				@Override public CellType type() 
				{ 
					return func.returnType();
				}
				@Override public boolean equals(Object that)
				{
					if (this == that) return true;
					if (that == null) return false;
					if (!(that instanceof FunctionExpr)) return false;
					final FunctionExpr fe = (FunctionExpr)that;
					return equal(functionName(),fe.functionName()) && equal(type(),fe.type()) && listsEql(args(), fe.args(), exprEqlPredicate);
				}

				@Override public int hashCode() { return hash(functionName(),type()) + deepHashCode(args().toArray().array()); }
				@Override public String toString()
				{
                    final String argsString = args().foldRight(fj((exp, accum) -> exp.toString() + "," + accum),"");
					return functionName() + "(" + argsString.substring(0, argsString.length()-1) + ")";
				}
			}
			
			@Override public FunctionExpr withArgs(final Expr... _args) { return new FunctionExprImpl(List.list(_args)); }
			@Override public FunctionExpr withArgs(final List<? extends Expr> args) { return new FunctionExprImpl(args); }
		};
	}

	public interface BindBuilder
	{
		Binding to(final Expr expr);
	}
	
	public BindBuilder bindingOf(final VarExpr varExpr)
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
					@Override public boolean equals(Object that)
					{
                        return this == that ||
                               that != null &&
                               that instanceof Binding &&
                               equal(var(),((Binding)that).var()) && equal(expression(),((Binding)that).expression());
					}
					
					@Override public int hashCode() { return hash(var()) + hash(expression()); }
					@Override public String toString() { return var().toString() + " = " + expression().toString(); }
					
				};
			}
		};
	}

	private static final class CompositeSequence implements CompositeExpr
	{
		private final List<Expr> expressions;
		CompositeSequence(final List<Expr> _exprs)
		{
			checkArgument(_exprs != null,"Can't have a null expression list for sequence");
			this.expressions = _exprs;
		}
		@Override public List<Expr> subExpressions() { return expressions; }
		@Override public CellType type() { return subExpressions().last().type(); }
		@Override public boolean equals(Object that)
		{
            return this == that ||
                   that != null &&
                   that instanceof CompositeExpr &&
                   listsEql(subExpressions(),((CompositeExpr)that).subExpressions(),exprEqlPredicate);
		}
		
		@Override public int hashCode() { return deepHashCode(subExpressions().toArray().array()); }
		@Override public String toString()
		{
            return subExpressions().foldRight(fj((exp,accum)->exp.toString() + ";\n" + accum),"");
		}
	}
	
	public CompositeExpr sequence(final Expr... expressions) { return new CompositeSequence(list(expressions)); }
	
	public CompositeExpr sequence(final List<Expr> expressions) { return new CompositeSequence(expressions); }

	//----
	public interface BranchBuilder
	{
		BranchBuilder ifTrue(Expr e);
		BranchExpr ifFalse(Expr e);
	}
	
	public BranchBuilder test(final Expr testExpr)
	{
		checkArgument(testExpr != null,"Test expression can't be null");
		checkArgument(testExpr.type().equals(BOOLEAN),"Test expression must boolean");
		
		return new BranchBuilder() {

			Expr trueExpr;
			@Override public BranchBuilder ifTrue(final Expr e)
			{
				checkArgument(e != null,"True expression can't be null");
				trueExpr = e;
				return this;
			}

			@Override public BranchExpr ifFalse(final Expr e)
			{
				checkArgument(e != null,"False expression can't be null");
				checkArgument(e.type().equals(trueExpr.type()),"True and False result expression must have the same type");
				return new BranchExpr() {

					@Override public Expr test() { return testExpr; }
					@Override public Expr whenTrue() { return trueExpr; }
					@Override public Expr whenFalse() { return e; }
					@Override
					public boolean equals(Object that)
					{
                        return this == that ||
                                that != null &&
                               that instanceof BranchExpr &&
                               test().equals(((BranchExpr)that).test()) &&
                               whenTrue().equals(((BranchExpr)that).whenTrue()) &&
                               whenFalse().equals(((BranchExpr)that).whenFalse());
					}
					
					@Override public int hashCode() { return hash(test()) + hash(whenTrue()) + hash(whenFalse()); }
					@Override public String toString()
					{
						return format("if (%1$s) then (%2$s) else (%3$s)", test().toString(),whenTrue().toString(),whenFalse().toString());
					}
					
					@Override public CellType type() { return whenTrue().type(); } //must be the same as the false branch;
				};
			}};
	}

}
