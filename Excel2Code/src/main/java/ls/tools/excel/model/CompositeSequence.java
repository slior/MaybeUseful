package ls.tools.excel.model;

import fj.data.List;
import ls.tools.excel.CellType;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.deepHashCode;
import static ls.tools.fj.Util.*;

/**
*/
final class CompositeSequence implements CompositeExpr
{
    private final List<Expr> expressions;
    public CompositeSequence(final List<Expr> _exprs)
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
                        listsEql(subExpressions(),((CompositeExpr)that).subExpressions(),nullCheckingEqualPredicate());
    }

    @Override public int hashCode() { return deepHashCode(subExpressions().toArray().array()); }
    @Override public String toString()
    {
        return subExpressions().foldRight(fj((exp,accum)->exp.toString() + ";\n" + accum),"");
    }
}
