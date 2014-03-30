package ls.tools.excel.model;

import ls.tools.excel.CellType;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.hash;
import static ls.tools.excel.CellType.BOOLEAN;

/**
* An implementation of @LiteralExpr
*/
final class LiteralExprImpl implements LiteralExpr
{
    private final String val;
    private final CellType type;

    public LiteralExprImpl(final String _val, final CellType _type)
    {
        checkArgument(_val != null,"Literal value can't be null");

        this.val = _val;
        this.type = _type;
    }

    public LiteralExprImpl(final boolean _val) { this(String.valueOf(_val),CellType.BOOLEAN); }
    public LiteralExprImpl(final Number _val) { this(String.valueOf(_val),CellType.NUMERIC); }

    @Override public CellType type() { return type; }
    @Override public String value() { return val; }

    @Override
    public boolean equals(Object that)
    {
        return this == that ||
                that != null &&
                that instanceof LiteralExpr &&
                equal(type(),((LiteralExpr)that).type()) &&
                type().equals(BOOLEAN) ?
                    equal(value().toLowerCase(),((LiteralExpr)that).value().toLowerCase()) :
                    equal(value(),((LiteralExpr)that).value());
    }

    @Override public int hashCode() { return hash(type(),value()); }
    @Override public String toString() { return value(); }
} //end of LiteralExprImpl class
