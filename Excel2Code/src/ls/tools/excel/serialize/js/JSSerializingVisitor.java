package ls.tools.excel.serialize.js;

import static com.google.common.base.Preconditions.checkArgument;
import static ls.tools.excel.CellType.STRING;
import ls.tools.excel.model.BinOpExpr;
import ls.tools.excel.model.Binding;
import ls.tools.excel.model.CompositeExpr;
import ls.tools.excel.model.Expr;
import ls.tools.excel.model.ExprVisitorTrait.ExprVisitor;
import ls.tools.excel.model.FunctionExpr;
import ls.tools.excel.model.LiteralExpr;
import ls.tools.excel.model.VarExpr;
import fj.Effect;
import fj.data.List;

public final class JSSerializingVisitor implements ExprVisitor
{

	private static final String NL = "\n";
	private final StringBuilder buffer = new StringBuilder();
	
	private JSSerializingVisitor w(final String s) { buffer.append(s); return this; }
	private JSSerializingVisitor w(final char c) { buffer.append(c); return this; }
	
	@Override
	public void visit(final CompositeExpr ce)
	{
		checkArgument(ce != null,"Can't have a null expression to visit");
		final List<Expr> allButLast = ce.subExpressions().take(ce.subExpressions().length()-1); 
		w('{');
			allButLast.foreach(new Effect<Expr>() {
				@Override public void e(Expr a) {
					w(NL); a.accept(JSSerializingVisitor.this); }});
			w(NL).w("return "); ce.subExpressions().last().accept(this); w(';');
		w(NL).w('}');
	}

	@Override
	public void visit(final Binding b)
	{
		checkArgument(b != null,"Binding can't be null");
		w("var ").w(b.var().name()).w(" = ");
		b.expression().accept(this);
		w(';');
	}

	@Override
	public void visit(final BinOpExpr bo)
	{
		checkArgument(bo != null,"Expr can't be null");
		bo.subExpressions().head().accept(this);
		w(bo.op());
		bo.subExpressions().last().accept(this);
	}

	@Override
	public void visit(final FunctionExpr fe)
	{
		checkArgument(fe != null,"Expr can't be null");
		w(fe.functionName()).w('(');
			fe.args().foreach(new Effect<Expr>() {
				@Override public void e(Expr arg) {
					arg.accept(JSSerializingVisitor.this);
					w(',');
				}
			});
		buffer.deleteCharAt(buffer.length()-1); //drop last comma
		w(')').w(';');
		
	}

	@Override
	public void visit(LiteralExpr le)
	{
		if (le.type() == STRING) w("'");
		w(le.value());
		if (le.type() == STRING) w("'");
	}

	@Override
	public void visit(VarExpr ve)
	{
		w(ve.name());
	}
	
	String result() { return buffer.toString(); }

}
