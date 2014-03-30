package ls.tools.excel.model;

import fj.data.List;
import ls.tools.excel.CellType;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.deepHashCode;
import static java.util.Objects.hash;
import static ls.tools.fj.Util.*;

/**
* Created by lior on 3/30/2014.
*/
final class FunctionExprImpl implements FunctionExpr
{
    private final List<Expr> args;
    private final Function func;

    @SuppressWarnings("unchecked")
    FunctionExprImpl(final Function _func, final List<? extends Expr> _args)
    {
        checkArgument(_func != null,"Function can't be null");
        this.func = _func;
        this.args = (List<Expr>) (_args == null ? List.nil() : _args);
    }

    @Override public String functionName() { return func.name(); }
    @Override public List<Expr> args() { return args; }
    @Override public CellType type()
    {
        return func.returnType();
    }
    @Override public boolean equals(Object that)
    {
        return this == that ||
                that != null &&
                that instanceof FunctionExpr &&
                equal(functionName(),((FunctionExpr)that).functionName()) &&
                equal(type(),((FunctionExpr)that).type()) &&
                listsEql(args(), ((FunctionExpr) that).args(), nullCheckingEqualPredicate());
    }

    @Override public int hashCode() { return hash(functionName(),type()) + deepHashCode(args().toArray().array()); }
    @Override public String toString()
    {
        final String argsString = args().foldRight(fj((exp, accum) -> exp.toString() + "," + accum),"");
        return functionName() + "(" + argsString.substring(0, argsString.length()-1) + ")";
    }
}
