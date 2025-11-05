import java.util.*;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	final Environment globals = new Environment();
	private Environment env = globals;

	public Interpreter() {
		Builtins.registerRuntime(globals);	
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
		// If value is 0, then treat it falsy
		if (object instanceof NumericValue && ((NumericValue)object).value == 0) return false;
		// Otherwise true
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
		Object leftRaw = evaluate(expr.left);
		Object rightRaw = evaluate(expr.right);

		// Equality and string concat are handled separately:
		if (expr.operator.type == TokenType.EQUAL_EQUAL) return isEqual(leftRaw, rightRaw);
		if (expr.operator.type == TokenType.BANG_EQUAL) return !isEqual(leftRaw, rightRaw);

		// String concatenation
		if (expr.operator.type == TokenType.PLUS && leftRaw instanceof String) {
			return (String)leftRaw + stringify(rightRaw);
		}

		// Numeric domain operations
		NumericValue left = asNum(leftRaw, expr.operator);
		NumericValue right = asNum(rightRaw, expr.operator);

		Type L = left.type;
		Type R = right.type;

		switch (expr.operator.type) {
			// Inequalities
			case GREATER:
			case GREATER_EQUAL:
			case LESS:
			case LESS_EQUAL:
				// Must be numeric AND same type
				if (!L.isNumericDomain() || !R.isNumericDomain() || !L.equals(R)) {
					throw new RainRuntimeError(expr.operator,
						"Relational operators require matching numeric types; got " + L + " and " + R + ".");
				}

				switch (expr.operator.type) {
					case GREATER:       return left.value >  right.value;
					case GREATER_EQUAL: return left.value >= right.value;
					case LESS:          return left.value <  right.value;
					case LESS_EQUAL:    return left.value <= right.value;
				}
				// (unreachable)
				break;

			// Addition
			case PLUS:
				if (L.equals(Type.volume()) && R.equals(Type.volume()))
					return new NumericValue(Type.volume(), left.value + right.value);
				if (L.equals(Type.area()) && R.equals(Type.area()))
					return new NumericValue(Type.area(), left.value + right.value);
				if (L.equals(Type.rain()) && R.equals(Type.rain()))
					return new NumericValue(Type.rain(), left.value + right.value);
				throw new RainRuntimeError(expr.operator, "Invalid + between " + L + " and " + R + ".");

			// Subtraction
			case MINUS:
				if (L.equals(Type.volume()) && R.equals(Type.volume()))
					return new NumericValue(Type.volume(), left.value - right.value);
				if (L.equals(Type.area()) && R.equals(Type.area()))
					return new NumericValue(Type.area(), left.value - right.value);
				if (L.equals(Type.rain()) && R.equals(Type.rain()))
					return new NumericValue(Type.rain(), left.value - right.value);
				throw new RainRuntimeError(expr.operator, "Invalid - between " + L + " and " + R + ".");

			// Multiplication
			case STAR:
				// Val scaling
				if (L.equals(Type.val()) && R.isNumericDomain())
					return new NumericValue(R, left.value * right.value);
				if (R.equals(Type.val()) && L.isNumericDomain())
					return new NumericValue(L, left.value * right.value);
				// Area * Rain = Volume
				if (L.equals(Type.area()) && R.equals(Type.rain()))
					return new NumericValue(Type.volume(), left.value * right.value);
				if (L.equals(Type.rain()) && R.equals(Type.area()))
					return new NumericValue(Type.volume(), left.value * right.value);

				throw new RainRuntimeError(expr.operator, "Invalid * between " + L + " and " + R + ".");

			// Division
			case SLASH:
				if (right.value == 0)
					throw new RainRuntimeError(expr.operator, "Division by zero.");
				// X / Val = X
				if (R.equals(Type.val()) && L.isNumericDomain())
					return new NumericValue(L, left.value / right.value);
				// Volume / Rain = Area
				if (L.equals(Type.volume()) && R.equals(Type.rain()))
					return new NumericValue(Type.area(), left.value / right.value);
				// Volume / Area = Rain
				if (L.equals(Type.volume()) && R.equals(Type.area()))
					return new NumericValue(Type.rain(), left.value / right.value);
				throw new RainRuntimeError(expr.operator, "Invalid / between " + L + " and " + R + ".");
		}
		// unreachable
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

		double raw = ((Number)expr.value).doubleValue();
		String suffix = expr.unit != null ? expr.unit.lexeme : null;

		Type type = Type.val();
		double scaled = raw;

		if (suffix != null) {
			switch (suffix) {
				case "L":  type = Type.volume(); scaled = raw; break;
				case "kL": type = Type.volume(); scaled = raw * 1_000; break;
				case "ML": type = Type.volume(); scaled = raw * 1_000_000; break;
				case "GL": type = Type.volume(); scaled = raw * 1_000_000_000; break;
				case "TL": type = Type.volume(); scaled = raw * 1_000_000_000_000.0; break;
				case "mm": type = Type.rain();   scaled = raw; break;
				case "m2": type = Type.area();   scaled = raw; break;
				case "km2":type = Type.area();   scaled = raw * 1_000_000; break;
				case "%":  type = Type.val();    scaled = raw * 0.01; break;
				default:
					throw new RainRuntimeError(expr.unit, "Unknown literal unit '" + suffix + "'");
			}
		}

		return new NumericValue(type, scaled);
	}

	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		Object rightRaw = evaluate(expr.right);
		NumericValue right = asNum(rightRaw, expr.operator);

		switch (expr.operator.type) {
			case TokenType.BANG:
				return !isTruthy(right.value);
			case TokenType.MINUS:
				return new NumericValue(right.type, -right.value);
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
	private NumericValue asNum(Object v, Token op) {
		if (!(v instanceof NumericValue)) {
			throw new RainRuntimeError(op, "Expected a numeric value, got " + v);
		}
		return (NumericValue)v;
	}
	public String display(Object value) {
		return stringify(value);
	}

	public boolean truthy(Object value) {
		return isTruthy(value);
	}
}