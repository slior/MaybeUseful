package ls.tools.excel.model;

import static com.google.common.base.Preconditions.checkArgument;

public final aspect ExprVisitorTrait 
{
	public interface ExprVisitor
	{
		void visit(CompositeExpr ce);
		void visit(Binding b);
		void visit(BinOpExpr bo);
		void visit(FunctionExpr fe);
		void visit(LiteralExpr le);
		void visit(VarExpr ve);
	}
	
	public void Expr.accept(final ExprVisitor v)
	{
		checkArgument(v != null,"Visitor can't be null");
		if (this instanceof Binding) v.visit((Binding)this);
		else if (this instanceof BinOpExpr) v.visit((BinOpExpr)this); //must be considered before CompositeExpr
		else if (this instanceof FunctionExpr) v.visit((FunctionExpr)this);
		else if (this instanceof LiteralExpr) v.visit((LiteralExpr)this);
		else if (this instanceof VarExpr) v.visit((VarExpr)this);
		else if (this instanceof CompositeExpr) v.visit((CompositeExpr)this);
		else throw new IllegalArgumentException("Can't identify type of expression: " + getClass().getCanonicalName());
	}
}
