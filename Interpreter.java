class Interpreter implements Expr.Visitor<Object> {
	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		return null;
	}
	@Override
	public Object visitGroupingExpr(Expr.Grouping expr) {
		return null;
	}
	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		return expr.value;
	}
	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		return null;
	}
	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		return null;
	}
	@Override
	public Object visitAssignExpr(Expr.Assign expr) {
		return null;
	}
	@Override
	public Object visitLogicalExpr(Expr.Logical expr) {
		return null;
	}
	@Override
	public Object visitCallExpr(Expr.Call expr) {
		return null;
	}
	@Override
	public Object visitGetExpr(Expr.Get expr) {
		return null;
	}
	@Override
	public Object visitSetExpr(Expr.Set expr) {
		return null;
	}
	@Override
	public Object visitIndexExpr(Expr.Index expr) {
		return null;
	}
	@Override
	public Object visitIndexSetExpr(Expr.IndexSet expr) {
		return null;
	}
	@Override
	public Object visitArrayExpr(Expr.Array expr) {
		return null;
	}
	@Override
	public Object visitThisExpr(Expr.This expr) {
		return null;
	}
}