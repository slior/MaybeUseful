package ls.tools.excel.model;

import fj.data.List;
import ls.tools.excel.CellType;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static fj.data.List.list;
import static java.lang.String.format;
import static java.util.Arrays.deepHashCode;
import static java.util.Objects.hash;
import static ls.tools.excel.CellType.BOOLEAN;
import static ls.tools.fj.Util.*;

/**
 */
public interface ExpressionBuilder {

    public interface VarBuilder { VarExpr ofType(CellType _t); }

    /**
     * Enables <code>e().var("name").ofType("type")</code>
     */
    default VarBuilder var(final String _n)
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

    public interface LiteralBuilder { LiteralExpr ofType(CellType type); }

    /**
     * Enables <code>e().literal("val").ofType("type");</code>
     */
    default LiteralBuilder literal(final String val) { return (type) -> new LiteralExprImpl(val,type); }

    /**
     * A syntactic sugar for <code>literal("num").ofType(NUMERIC)</code>
     * @param num The number for the literal
     * @return A new {@link LiteralExpr}.
     * @see #literal(String)
     */
    default LiteralExpr numericLiteral(final Number num) { return new LiteralExprImpl(num); }
    /**
     * A syntactic sugar for <code>literal("boolean value").ofType(BOOLEAN)</code>
     * @param b The boolean value for the literal
     * @return A new {@link LiteralExpr}
     * @see #literal(String)
     */
    default LiteralExpr booleanLiteral(final boolean b) { return new LiteralExprImpl(b); }

    //----

    default BinOpExpr binOp(final Expr e1, final BinaryOp op, final Expr e2)
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
                        listsEql(this.subExpressions(),boe.subExpressions(),nullCheckingEqualPredicate());
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

    default FunctionInvocationBuilder invocationOf(final Function func)
    {
        checkArgument(func != null,"Function can't be null");
        return new FunctionInvocationBuilder()
        {
            @Override public FunctionExpr withArgs(final Expr... _args) { return new FunctionExprImpl(func, list(_args)); }
            @Override public FunctionExpr withArgs(final List<? extends Expr> args) { return new FunctionExprImpl(func, args); }
        };
    }

    //----

    public interface BindBuilder
    {
        Binding to(final Expr expr);
    }

    default BindBuilder bindingOf(final VarExpr varExpr)
    {
        checkArgument(varExpr != null,"Var can't be null for binding");
        return expr ->
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
            };
    }

    //----

    default CompositeExpr sequence(final Expr... expressions) { return new CompositeSequence(list(expressions)); }

    default CompositeExpr sequence(final List<Expr> expressions) { return new CompositeSequence(expressions); }

    //----
    public interface BranchBuilder
    {
        BranchBuilder ifTrue(Expr e);
        BranchExpr ifFalse(Expr e);
    }

    default BranchBuilder test(final Expr testExpr)
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
