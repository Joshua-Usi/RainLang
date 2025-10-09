// AstPrinter.java
import java.util.List;
import java.util.stream.Collectors;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
	String printExpr(Expr expr) { return expr.accept(this); }

	String printStmts(List<Stmt> stmts) {
		return stmts.stream().map(s -> s.accept(this)).collect(Collectors.joining("\n"));
	}

	@Override
	public String visitBinaryExpr(Expr.Binary expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitGroupingExpr(Expr.Grouping expr) {
		return parenthesize("group", expr.expression);
	}

	@Override
	public String visitLiteralExpr(Expr.Literal expr) {
		if (expr.value == null) return "None";
		if (expr.unit != null)	return expr.value.toString() + expr.unit.lexeme;
		return expr.value.toString();
	}

	@Override
	public String visitUnaryExpr(Expr.Unary expr) {
		return parenthesize(expr.operator.lexeme, expr.right);
	}

	@Override
	public String visitVariableExpr(Expr.Variable expr) {
		return expr.name.lexeme;
	}

	@Override
	public String visitAssignExpr(Expr.Assign expr) {
		return parenthesize("=", new Expr.Variable(expr.name), expr.value);
	}

	@Override
	public String visitLogicalExpr(Expr.Logical expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitCallExpr(Expr.Call expr) {
		String args = expr.arguments.stream().map(a -> a.accept(this)).collect(Collectors.joining(" "));
		return "(" + "call " + expr.callee.accept(this) + (args.isEmpty() ? "" : " " + args) + ")";
	}

	@Override
	public String visitGetExpr(Expr.Get expr) {
		return "(get " + expr.object.accept(this) + " ." + expr.name.lexeme + ")";
	}

	@Override
	public String visitSetExpr(Expr.Set expr) {
		return "(set " + expr.object.accept(this) + " ." + expr.name.lexeme + " " + expr.value.accept(this) + ")";
	}

	@Override
	public String visitIndexExpr(Expr.Index expr) {
		return "(index " + expr.array.accept(this) + " " + expr.index.accept(this) + ")";
	}

	@Override
	public String visitIndexSetExpr(Expr.IndexSet expr) {
		return "(index-set " + expr.array.accept(this) + " " + expr.index.accept(this) + " " + expr.value.accept(this) + ")";
	}

	@Override
	public String visitArrayExpr(Expr.Array expr) {
		String els = expr.elements.stream().map(e -> e.accept(this)).collect(Collectors.joining(" "));
		return "(array " + els + ")";
	}

	@Override
	public String visitThisExpr(Expr.This expr) {
		return "this";
	}

	private String parenthesize(String name, Expr... exprs) {
		StringBuilder builder = new StringBuilder();
		builder.append("(").append(name);
		for (Expr expr : exprs) {
			builder.append(" ").append(expr.accept(this));
		}
		builder.append(")");
		return builder.toString();
	}

	@Override
	public String visitExpressionStmt(Stmt.Expression stmt) {
		return "(; " + stmt.expression.accept(this) + ")";
	}

	@Override
	public String visitVarDeclStmt(Stmt.VarDecl stmt) {
		if (stmt.initializer == null) {
			return "(var " + stmt.type + " " + stmt.name.lexeme + ")";
		}
		return "(var " + stmt.type + " " + stmt.name.lexeme + " " + stmt.initializer.accept(this) + ")";
	}

	@Override
	public String visitFunctionStmt(Stmt.Function stmt) {
		String params = stmt.params.stream()
				.map(p -> p.type.toString() + " " + p.name.lexeme)
				.collect(Collectors.joining(" "));
		return "(fun " + stmt.returnType + " " + stmt.name.lexeme + " (" + params + ") " + printBlockInline(stmt.body) + ")";
	}

	@Override
	public String visitClassStmt(Stmt.ClassStmt stmt) {
		String members = stmt.members.stream().map(m -> m.accept(this)).collect(Collectors.joining(" "));
		return "(class " + stmt.name.lexeme + " " + members + ")";
	}

	@Override
	public String visitBlockStmt(Stmt.Block stmt) {
		return printBlockInline(stmt.statements);
	}

	@Override
	public String visitIfStmt(Stmt.If stmt) {
		String s = "(if " + stmt.condition.accept(this) + " " + stmt.thenBranch.accept(this);
		if (stmt.elseBranch != null) s += " " + stmt.elseBranch.accept(this);
		return s + ")";
	}

	@Override
	public String visitWhileStmt(Stmt.While stmt) {
		return "(while " + stmt.condition.accept(this) + " " + stmt.body.accept(this) + ")";
	}

	@Override
	public String visitForStmt(Stmt.For stmt) {
		String init	= stmt.initializer == null ? "null" : stmt.initializer.accept(this);
		String cond	= stmt.condition	 == null ? "true" : stmt.condition.accept(this);
		String incr	= stmt.increment	 == null ? "null" : stmt.increment.accept(this);
		return "(for " + init + " " + cond + " " + incr + " " + stmt.body.accept(this) + ")";
	}

	@Override
	public String visitReturnStmt(Stmt.Return stmt) {
		String v = (stmt.value == null) ? "" : " " + stmt.value.accept(this);
		return "(return" + v + ")";
	}

	private String printBlockInline(List<Stmt> body) {
		return "(block " + body.stream().map(s -> s.accept(this)).collect(Collectors.joining(" ")) + ")";
	}

	@Override
	public String visitFieldStmt(Stmt.Field stmt) {
		if (stmt.initializer == null) {
			return "(field " + stmt.type + " " + stmt.name.lexeme + ")";
		}
		return "(field " + stmt.type + " " + stmt.name.lexeme + " " + stmt.initializer.accept(this) + ")";
	}

	@Override
	public String visitConstructorStmt(Stmt.Constructor stmt) {
		StringBuilder b = new StringBuilder();
		b.append("(constructor ").append(stmt.name.lexeme).append(" (");
		for (Stmt.Param p : stmt.params) {
			b.append(p.type).append(" ").append(p.name.lexeme).append(" ");
		}
		b.append(")");
		for (Stmt s : stmt.body) {
			b.append(" ").append(s.accept(this));
		}
		b.append(")");
		return b.toString();
	}
}