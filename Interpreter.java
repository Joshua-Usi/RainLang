import java.util.*;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	final Environment globals = new Environment();
	private Environment env = globals;

	private boolean replMode = false;

	public Interpreter() {
		Builtins.registerRuntime(globals);	
	}

	public void setReplMode(boolean enabled) { this.replMode = enabled; }

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

		if (value instanceof List<?>) {
			List<?> list = (List<?>) value;
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			for (int i = 0; i < list.size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(stringify(list.get(i)));
			}
			sb.append("]");
			return sb.toString();
		}

		if (value instanceof RainInstance) return value.toString();
		if (value instanceof RainClass)    return value.toString();

		return value.toString();
	}
	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object leftRaw = evaluate(expr.left);
		Object rightRaw = evaluate(expr.right);

		if (expr.operator.type == TokenType.EQUAL_EQUAL) return isEqual(leftRaw, rightRaw);
		if (expr.operator.type == TokenType.BANG_EQUAL) return !isEqual(leftRaw, rightRaw);

		if (expr.operator.type == TokenType.PLUS && leftRaw instanceof List && rightRaw instanceof List) {
			List<?> L = (List<?>) leftRaw;
			List<?> R = (List<?>) rightRaw;
			List<Object> out = new ArrayList<>(L.size() + R.size());
			out.addAll(L);
			out.addAll(R);
			return out;
		}

		if (expr.operator.type == TokenType.PLUS && leftRaw instanceof String) {
			return (String)leftRaw + stringify(rightRaw);
		}

		NumericValue left = asNum(leftRaw, expr.operator);
		NumericValue right = asNum(rightRaw, expr.operator);

		Type L = left.type;
		Type R = right.type;

		switch (expr.operator.type) {
			case GREATER:
			case GREATER_EQUAL:
			case LESS:
			case LESS_EQUAL:
				switch (expr.operator.type) {
					case GREATER:       return left.value >  right.value;
					case GREATER_EQUAL: return left.value >= right.value;
					case LESS:          return left.value <  right.value;
					case LESS_EQUAL:    return left.value <= right.value;
				}
				break;

			case PLUS:
				return new NumericValue(left.type, left.value + right.value);

			case MINUS:
				return new NumericValue(left.type, left.value - right.value);

			case STAR:
				if (L.equals(Type.val()) && R.isNumericDomain())
					return new NumericValue(R, left.value * right.value);
				if (R.equals(Type.val()) && L.isNumericDomain())
					return new NumericValue(L, left.value * right.value);
				if (L.equals(Type.area()) && R.equals(Type.rain()))
					return new NumericValue(Type.volume(), left.value * right.value);
				if (L.equals(Type.rain()) && R.equals(Type.area()))
					return new NumericValue(Type.volume(), left.value * right.value);

				throw new RainRuntimeError(expr.operator, "Invalid * between " + L + " and " + R + ".");

			case SLASH:
				if (right.value == 0)
					throw new RainRuntimeError(expr.operator, "Division by zero.");
				if (R.equals(Type.val()) && L.isNumericDomain())
					return new NumericValue(L, left.value / right.value);
				if (L.equals(Type.volume()) && R.equals(Type.rain()))
					return new NumericValue(Type.area(), left.value / right.value);
				if (L.equals(Type.volume()) && R.equals(Type.area()))
					return new NumericValue(Type.rain(), left.value / right.value);
				throw new RainRuntimeError(expr.operator, "Invalid / between " + L + " and " + R + ".");
		}
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

		switch (expr.operator.type) {
			case TokenType.BANG:
				return !isTruthy(rightRaw);
			case TokenType.MINUS: {
				NumericValue right = asNum(rightRaw, expr.operator);
				return new NumericValue(right.type, -right.value);
			}
			case TokenType.PLUS: {
				return asNum(rightRaw, expr.operator);
			}
		}

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
		return evaluate(expr.right);
	}

	@Override
	public Object visitCallExpr(Expr.Call expr) {
		Object callee = evaluate(expr.callee);

		List<Object> arguments = new ArrayList<>();
		for (Expr arg : expr.arguments) {
			arguments.add(evaluate(arg));
		}

		if (callee instanceof OverloadSet set) {
			CallResolution.Resolved r = CallResolution.get(expr);
			if (r == null)
				throw new RainRuntimeError(expr.paren, "Internal error: unresolved overloaded call.");
			if (r.slot < 0 || r.slot >= set.size())
				throw new RainRuntimeError(expr.paren, "Internal error: overload slot out of range.");
			RainFunction fn = set.get(r.slot);
			return fn.call(this, expr.paren, arguments);
		}

		if (!(callee instanceof Callable)) {
			throw new RainRuntimeError(expr.paren, "Can only call functions and classes.");
		}

		Callable function = (Callable) callee;

		if (arguments.size() != function.arity()) {
			throw new RainRuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
		}

		return function.call(this, expr.paren, arguments);
	}
	@SuppressWarnings("unchecked")
	@Override
	public Object visitGetExpr(Expr.Get expr) {
		Object object = evaluate(expr.object);

		if (object instanceof RainInstance inst) {
			return inst.get(expr.name);
		}

		if (object instanceof List<?> base) {
			String m = expr.name.lexeme;
			switch (m) {
				case "length":
					return new NumericValue(Type.val(), base.size());

				case "push":
					return new Callable() {
						@Override public int arity() { return 1; }
						@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) {
							((List<Object>) base).add(args.get(0));
							return new NumericValue(Type.val(), base.size());
						}
						@Override public String toString() { return "<native Array.push>"; }
					};

				case "pop":
					return new Callable() {
						@Override public int arity() { return 0; }
						@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) {
							if (base.isEmpty())
								throw new RainRuntimeError(expr.name, "Array.pop() on empty array.");
							return ((List<Object>) base).remove(base.size() - 1);
						}
						@Override public String toString() { return "<native Array.pop>"; }
					};

				case "clear":
					return new Callable() {
						@Override public int arity() { return 0; }
						@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) {
							((List<Object>) base).clear();
							return null;
						}
						@Override public String toString() { return "<native Array.clear>"; }
					};

				case "insert":
					return new Callable() {
						@Override public int arity() { return 2; }
						@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) {
							int i = interpreter.asIndex(args.get(0), expr.name);
							List<Object> l = (List<Object>) base;
							if (i < 0 || i > l.size())
								throw new RainRuntimeError(expr.name, "Index " + i + " out of bounds for insert length " + l.size() + ".");
							l.add(i, args.get(1));
							return null;
						}
						@Override public String toString() { return "<native Array.insert>"; }
					};

				case "removeAt":
					return new Callable() {
						@Override public int arity() { return 1; }
						@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) {
							int i = interpreter.asIndex(args.get(0), expr.name);
							List<Object> l = (List<Object>) base;
							if (i < 0 || i >= l.size())
								throw new RainRuntimeError(expr.name, "Index " + i + " out of bounds for length " + l.size() + ".");
							return l.remove(i);
						}
						@Override public String toString() { return "<native Array.removeAt>"; }
					};
			}
			throw new RainRuntimeError(expr.name, "Unknown array member '" + m + "'.");
		}

		if (object instanceof String s) {
			String m = expr.name.lexeme;
			switch (m) {
				case "length":
					return new NumericValue(Type.val(), s.length());
			}
			throw new RainRuntimeError(expr.name, "Unknown string member '" + m + "'.");
		}

		throw new RainRuntimeError(expr.name, "Only instances, arrays, and strings have properties.");
	}

	@Override
	public Object visitSetExpr(Expr.Set expr) {
		Object object = evaluate(expr.object);
		if (!(object instanceof RainInstance inst)) {
			throw new RainRuntimeError(expr.name, "Only instances have fields.");
		}
		Object value = evaluate(expr.value);
		inst.set(expr.name, value);
		return value;
	}
	@Override
	public Object visitIndexExpr(Expr.Index expr) {
		Object base = evaluate(expr.array);
		List<Object> list = asArray(base, expr.bracket);
		Object idxVal = evaluate(expr.index);
		int i = asIndex(idxVal, expr.bracket);
		if (i < 0 || i >= list.size()) {
			throw new RainRuntimeError(expr.bracket, "Index " + i + " out of bounds for length " + list.size() + ".");
		}
		return list.get(i);
	}
	@Override
	public Object visitIndexSetExpr(Expr.IndexSet expr) {
		Object base = evaluate(expr.array);
		List<Object> list = asArray(base, expr.bracket);
		int i = asIndex(evaluate(expr.index), expr.bracket);
		if (i < 0 || i >= list.size()) {
			throw new RainRuntimeError(expr.bracket, "Index " + i + " out of bounds for length " + list.size() + ".");
		}
		Object value = evaluate(expr.value);
		list.set(i, value);
		return value;
	}
	@Override
	public Object visitArrayExpr(Expr.Array expr) {
		List<Object> out = new ArrayList<>(expr.elements.size());
		for (Expr e : expr.elements) {
			out.add(evaluate(e));
		}
		return out;
	}
	@Override
	public Object visitThisExpr(Expr.This expr) {
		return env.get(expr.keyword);
	}
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		Object value = evaluate(stmt.expression);
		if (replMode && value != null) {
			System.out.println(stringify(value));
		}
		return null;
	}
	@Override
	public Void visitVarDeclStmt(Stmt.VarDecl stmt) {
		Object value = null;
		if (stmt.initializer != null) value = evaluate(stmt.initializer);
		env.define(stmt.name.lexeme, value);
		return null;
	}
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		RainFunction function = new RainFunction(stmt, env);

		if (env.hasLocal(stmt.name.lexeme)) {
			Object existing = env.getLocal(stmt.name.lexeme);
			if (existing instanceof OverloadSet set) {
				set.add(function);
			} else {
				OverloadSet set = new OverloadSet();
				set.add(function);
				env.define(stmt.name.lexeme, set);
			}
		} else {
			OverloadSet set = new OverloadSet();
			set.add(function);
			env.define(stmt.name.lexeme, set);
		}
		return null;
	}
	@Override
	public Void visitClassStmt(Stmt.ClassStmt stmt) {
		env.define(stmt.name.lexeme, null);

		Map<String, RainFunction> methods = new HashMap<>();
		List<String> fieldNames = new ArrayList<>();
		List<Stmt> fieldInits = new ArrayList<>();
		Stmt.Constructor ctor = null;

		Token thisTok = new Token(TokenType.THIS, "this", null, stmt.name.line, -1);

		for (Stmt m : stmt.members) {
			if (m instanceof Stmt.Function fn) {
				methods.put(fn.name.lexeme, new RainFunction(fn, env));
			} else if (m instanceof Stmt.Field f) {
				fieldNames.add(f.name.lexeme);
				if (f.initializer != null) {
					Expr set = new Expr.Set(new Expr.This(thisTok), f.name, f.initializer);
					fieldInits.add(new Stmt.Expression(set));
				}
			} else if (m instanceof Stmt.Constructor c) {
				ctor = c;
			}
		}

		RainClass k = new RainClass(stmt.name.lexeme, env, methods, fieldNames, fieldInits, ctor);
		env.assign(stmt.name, k);
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
			try {
				execute(stmt.body);
			} catch (RainContinue c) {
				continue;
			} catch (RainBreak b) {
				break;
			}
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
				try {
					execute(stmt.body);
				} catch (RainContinue c) {
					// fall through to increment
				} catch (RainBreak b) {
					break;
				}
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
	@SuppressWarnings("unchecked")
	private List<Object> asArray(Object v, Token at) {
		if (!(v instanceof List)) {
			throw new RainRuntimeError(at, "Expected an array, got " + stringify(v));
		}
		return (List<Object>) v;
	}
	private int asIndex(Object v, Token at) {
		NumericValue n = asNum(v, at);
		if (!n.type.equals(Type.val())) {
			throw new RainRuntimeError(at, "Array index must be Val, got " + n.type + ".");
		}
		double d = n.value;
		if (Double.isNaN(d) || Double.isInfinite(d) || Math.floor(d) != d) {
			throw new RainRuntimeError(at, "Array index must be an integer, got " + stringify(n));
		}
		if (d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
			throw new RainRuntimeError(at, "Array index out of range: " + stringify(n));
		}
		return (int) d;
	}
	public String display(Object value) {
		return stringify(value);
	}

	public boolean truthy(Object value) {
		return isTruthy(value);
	}

	@Override
	public Void visitBreakStmt(Stmt.Break stmt) {
		throw new RainBreak();
	}

	@Override
	public Void visitContinueStmt(Stmt.Continue stmt) {
		throw new RainContinue();
	}

}