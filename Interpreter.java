import java.util.*;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	final Environment globals = new Environment();
	private Environment env = globals;

	public Interpreter() {
		// Built ins
		globals.define("print", new Callable() {
			@Override
			public int arity() { return 1; }
			@Override
			public Object call(Interpreter interpreter, Token paren, List<Object> args) {
				System.out.println(stringify(args.get(0)));
				return null;
			}
			@Override
			public String toString() { return "<native fn print>"; }
		});
		globals.define("assert", new Callable() {
			@Override
			public int arity() { return 1; }
			@Override
			public Object call(Interpreter interpreter, Token paren, List<Object> args) {
				Object value = args.get(0);
				if (!isTruthy(value)) {
					throw new RainRuntimeError(paren, "Assertion failed: " + stringify(value));
				}
				return null;
			}
			@Override
			public String toString() { return "<native fn assert>"; }
		});
	}

	public void interpret(List<Stmt> statements) {
		try {
			for (Stmt stmt : statements) {
				execute(stmt);
			}
		} catch (RainRuntimeError error) {
			RainLang.error(error.token.line, error.getMessage());
		}
	}
	private void execute(Stmt stmt) {
		stmt.accept(this);
	}
	public void executeBlock(List<Stmt> statements, Environment newEnv) {
		Environment previous = env;
		try {
			env = newEnv;
			for (Stmt stmt : statements) {
				execute(stmt);
			}
		} finally {
			// Restore old environment
			env = previous;
		}
	}
	private Object evaluate(Expr expr) {
		return expr.accept(this);
	}
	private boolean isTruthy(Object object) {
		if (object == null) return false;
		if (object instanceof Boolean) return (boolean)object;
		// Everything is truthy for now
		return true;
	}
	private boolean isEqual(Object a, Object b) {
		if (a == null && b == null) return true;
		if (a == null) return false;

		return a.equals(b);
	}
	private String stringify(Object value) {
		if (value == null) return "none";

		// Clean up .0 for whole numbers
		if (value instanceof Double) {
			String text = value.toString();
			if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
			return text;
		}

		return value.toString();
	}
	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			// TODO, implement overloads for different types
			case TokenType.PLUS:
				return (double)left + (double)right;
			case TokenType.MINUS:
				return (double)left - (double)right;
			case TokenType.SLASH:
				if ((double)right == 0) {
					throw new RainRuntimeError(expr.operator, "Division by zero.");
				}
				return (double)left / (double)right;
			case TokenType.STAR:
				return (double)left * (double)right;
			case TokenType.GREATER:
				return (double)left > (double)right;
			case TokenType.GREATER_EQUAL:
				return (double)left >= (double)right;
			case TokenType.LESS:
				return (double)left < (double)right;
			case TokenType.LESS_EQUAL:
				return (double)left <= (double)right;
			case TokenType.BANG_EQUAL:
				return !isEqual(left, right);
			case TokenType.EQUAL_EQUAL:
				return isEqual(left, right);
		}

		// Unreachable
		return null;
	}
	@Override
	public Object visitGroupingExpr(Expr.Grouping expr) {
		return evaluate(expr.expression);
	}
	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		if (expr.value == null) return null;
	    if (expr.value instanceof Boolean) return expr.value;
	    if (expr.value instanceof String) return expr.value;
		
		Object value = expr.value;
		String literalSuffix = (expr.unit != null) ? expr.unit.lexeme : null;
		double multiplier = 1.0;

		if (literalSuffix != null) {
			switch (literalSuffix) {
				// Capacity
				case "L": multiplier = 1.0; break;
				case "kL": multiplier = 1_000.0; break;
				case "ML": multiplier = 1_000_000.0; break;
				case "GL": multiplier = 1_000_000_000.0; break;
				case "TL": multiplier = 1_000_000_000_000.0; break;
				// Percentage
				case "%": multiplier = 0.01; break;
				// Rain
				case "mm": multiplier = 1.0; break;
				// Area
				case "m2": multiplier = 1.0; break;
				case "km2": multiplier = 1_000_000.0; break;
				default:
					throw new RainRuntimeError(expr.unit, "Unknown literal");
			}
		}

		return (Object)((double)value * multiplier);
	}
	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			case TokenType.BANG:
				return !isTruthy(right);
			case TokenType.MINUS:
				return -(double)right;
		}

		// Unreachable
		return null;
	}
	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		return env.get(expr.name);
	}
	@Override
	public Object visitAssignExpr(Expr.Assign expr) {
		Object value = evaluate(expr.value);
		env.assign(expr.name, value);
		return value;
	}
	@Override
	public Object visitLogicalExpr(Expr.Logical expr) {
		Object left = evaluate(expr.left);

		if (expr.operator.type == TokenType.OR_OR) {
			if (isTruthy(left)) return left;
		} else if (expr.operator.type == TokenType.AND_AND) {
			if (!isTruthy(left)) return left;
		}
		// Only evaluate right side when needed
		return evaluate(expr.right);
	}

	@Override
	public Object visitCallExpr(Expr.Call expr) {
		Object callee = evaluate(expr.callee);

		List<Object> arguments = new ArrayList<>();
		for (Expr arg : expr.arguments) {
			arguments.add(evaluate(arg));
		}

		if (!(callee instanceof Callable)) {
			throw new RainRuntimeError(expr.paren, "Can only call functions and classes.");
		}

		Callable function = (Callable) callee;

		if (arguments.size() != function.arity()) {
			throw new RainRuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
		}

		// Finally, perform the call
		return function.call(this, expr.paren, arguments);
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
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		Object value = evaluate(stmt.expression);

		// Unreachable
		return null;
	}
	@Override
	public Void visitVarDeclStmt(Stmt.VarDecl stmt) {
		System.out.println(stmt.toString());
		Object value = null;
		if (stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}
		env.define(stmt.name.lexeme, value);

		// Unreachable
		return null;
	}
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		RainFunction function = new RainFunction(stmt, env);
		env.define(stmt.name.lexeme, function);
		return null;
	}
	@Override
	public Void visitClassStmt(Stmt.ClassStmt stmt) {
		return null;
	}
	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		executeBlock(stmt.statements, new Environment(env));
		return null;
	}
	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		Object condition = evaluate(stmt.condition);
		if (isTruthy(condition)) {
			execute(stmt.thenBranch);
		} else if (stmt.elseBranch != null) {
			execute(stmt.elseBranch);
		}
		return null;
	}
	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		while (isTruthy(evaluate(stmt.condition))) {
			execute(stmt.body);
		}
		return null;
	}
	@Override
	public Void visitForStmt(Stmt.For stmt) {
		Environment loopEnv = new Environment(env);
		try {
			env = loopEnv;
			if (stmt.initializer != null) {
				execute(stmt.initializer);
			}
			while (stmt.condition == null || isTruthy(evaluate(stmt.condition))) {
				execute(stmt.body);
				if (stmt.increment != null) {
					evaluate(stmt.increment);
				}
			}
		} finally {
			env = loopEnv.enclosing;
		}

		return null;
	}
	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		Object value = null;
		if (stmt.value != null) value = evaluate(stmt.value);
		// Actually crazy how returning a value is just throwing an Error
		throw new RainReturn(value);
	}

	@Override
	public Void visitFieldStmt(Stmt.Field stmt) {
		return null;
	}
	@Override
	public Void visitConstructorStmt(Stmt.Constructor stmt) {
		return null;
	}
}