import java.util.*;

class SemanticAnalyser implements Expr.Visitor<Type>, Stmt.Visitor<Void> {
	private final TypeEnvironment env = new TypeEnvironment();
	private final Deque<Type> returnStack = new ArrayDeque<>();
	private final Deque<Type> classStack = new ArrayDeque<>();
	private boolean firstEnter = true;

	void analyse(List<Stmt> program) {
		if (firstEnter) {
			Builtins.registerTypes(env);
			firstEnter = false;
		}
		for (Stmt s : program) visit(s);
	}

	// ---- Stmt Visitor ----
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		visit(stmt.expression);
		return null;
	}

	@Override
	public Void visitVarDeclStmt(Stmt.VarDecl stmt) {
		Type declared = resolveTypeNode(stmt.type);
		Type init = visit(stmt.initializer);
		if (!isAssignable(init, declared)) {
			RainLang.error(stmt.type.name.line,
				"Cannot assign " + init + " to variable '" + stmt.name.lexeme + "' of type " + declared + ".");
		}
		env.define(stmt.name.lexeme, declared);
		return null;
	}

	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		// Register name (calls are dynamically typed here)
		env.define(stmt.name.lexeme, Type.unknown());

		// New scope for params/body
		env.push();
		Type retType = resolveTypeNode(stmt.returnType);
		returnStack.push(retType);
		// Params
		for (Stmt.Param p : stmt.params) {
			Type pt = resolveTypeNode(p.type);
			env.define(p.name.lexeme, pt);
		}
		// Body
		for (Stmt s : stmt.body) visit(s);
		returnStack.pop();
		env.pop();
		return null;
	}

	@Override
	public Void visitClassStmt(Stmt.ClassStmt stmt) {
		Type cls = Type.classType(stmt.name.lexeme);
		env.define(stmt.name.lexeme, cls);

		// Class scope for fields/methods
		classStack.push(cls);
		env.push();
		env.define("this", cls);
		for (Stmt m : stmt.members) visit(m);
		env.pop();
		classStack.pop();
		return null;
	}

	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		env.push();
		for (Stmt s : stmt.statements) visit(s);
		env.pop();
		return null;
	}

	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		Type cond = visit(stmt.condition);
		requireBool(stmt.condition, cond, "If condition must be Bool, got " + cond + ".");
		visit(stmt.thenBranch);
		if (stmt.elseBranch != null) visit(stmt.elseBranch);
		return null;
	}

	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		Type cond = visit(stmt.condition);
		requireBool(stmt.condition, cond, "While condition must be Bool, got " + cond + ".");
		visit(stmt.body);
		return null;
	}

	@Override
	public Void visitForStmt(Stmt.For stmt) {
		env.push();
		if (stmt.initializer != null) visit(stmt.initializer);
		if (stmt.condition != null) {
			Type t = visit(stmt.condition);
			requireBool(stmt.condition, t, "For-loop condition must be Bool, got " + t + ".");
		}
		if (stmt.increment != null) visit(stmt.increment);
		visit(stmt.body);
		env.pop();
		return null;
	}

	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		Type expected = returnStack.isEmpty() ? Type.none() : returnStack.peek();
		if (stmt.value == null) {
			if (!expected.equals(Type.none())) {
				RainLang.error(stmt.keyword.line, "Return type mismatch: expected " + expected + " but returned None.");
			}
		} else {
			Type actual = visit(stmt.value);
			if (!isAssignable(actual, expected)) {
				RainLang.error(stmt.keyword.line, "Return type mismatch: expected " + expected + " but got " + actual + ".");
			}
		}
		return null;
	}

	@Override
	public Void visitFieldStmt(Stmt.Field stmt) {
		Type declared = resolveTypeNode(stmt.type);
		if (stmt.initializer != null) {
			Type init = visit(stmt.initializer);
			if (!isAssignable(init, declared)) {
				RainLang.error(stmt.type.name.line,
					"Cannot assign " + init + " to field '" + stmt.name.lexeme + "' of type " + declared + ".");
			}
		}
		// Fields are inside class scope already
		env.define(stmt.name.lexeme, declared);
		return null;
	}

	@Override
	public Void visitConstructorStmt(Stmt.Constructor stmt) {
		// New scope for params/body (inside class)
		env.push();
		for (Stmt.Param p : stmt.params) {
			Type pt = resolveTypeNode(p.type);
			env.define(p.name.lexeme, pt);
		}
		for (Stmt s : stmt.body) visit(s);
		env.pop();
		return null;
	}

	// ---- Expr Visitor ----
	@Override
	public Type visitBinaryExpr(Expr.Binary expr) {
		Type l = visit(expr.left);
		Type r = visit(expr.right);
		return binaryResult(expr.operator, l, r);
	}

	@Override
	public Type visitGroupingExpr(Expr.Grouping expr) {
		return visit(expr.expression);
	}

	@Override
	public Type visitLiteralExpr(Expr.Literal expr) {
		if (expr.value == null) return Type.none();
		if (expr.value instanceof Boolean) return Type.bool();
		if (expr.value instanceof String) return Type.string();

		// Numbers: decide by unit suffix
		Token unit = expr.unit;
		if (unit == null) return Type.val();

		String u = unit.lexeme;
		switch (u) {
			case "L": case "kL": case "ML": case "GL": case "TL":
				return Type.volume();
			case "mm":
				return Type.rain();
			case "m2": case "km2":
				return Type.area();
			case "%":
				return Type.val();
			default:
				RainLang.error(unit.line, "Unknown literal unit '" + u + "'.");
				return Type.unknown();
		}
	}

	@Override
	public Type visitUnaryExpr(Expr.Unary expr) {
		Type t = visit(expr.right);
		switch (expr.operator.type) {
			case TokenType.BANG:
				requireBool(expr.right, t, "Logical '!' requires Bool, got " + t + ".");
				return Type.bool();
			case TokenType.MINUS:
			case TokenType.PLUS:
				requireNumeric(expr.right, t, "Unary '" + expr.operator.lexeme + "' requires numeric domain, got " + t + ".");
				return t;
			default:
				return Type.unknown();
		}
	}

	@Override
	public Type visitVariableExpr(Expr.Variable expr) {
		Type t = env.lookup(expr.name.lexeme);
		if (t == null) {
			RainLang.error(expr.name.line, "Undefined variable '" + expr.name.lexeme + "'.");
			return Type.unknown();
		}
		return t;
	}

	@Override
	public Type visitAssignExpr(Expr.Assign expr) {
		Type target = env.lookup(expr.name.lexeme);
		if (target == null) {
			RainLang.error(expr.name.line, "Undefined variable '" + expr.name.lexeme + "'.");
			target = Type.unknown();
		}
		Type value = visit(expr.value);
		if (!isAssignable(value, target)) {
			RainLang.error(expr.name.line, "Cannot assign " + value + " to '" + expr.name.lexeme + "' of type " + target + ".");
		}
		return target;
	}

	@Override
	public Type visitLogicalExpr(Expr.Logical expr) {
		Type l = visit(expr.left);
		Type r = visit(expr.right);
		requireBool(expr.left, l, "Logical '" + expr.operator.lexeme + "' requires Bool operands (left is " + l + ").");
		requireBool(expr.right, r, "Logical '" + expr.operator.lexeme + "' requires Bool operands (right is " + r + ").");
		return Type.bool();
	}

	@Override
	public Type visitCallExpr(Expr.Call expr) {
		// Dynamically typed calls; validate args only
		for (Expr a : expr.arguments) visit(a);
		return Type.unknown();
	}

	@Override
	public Type visitGetExpr(Expr.Get expr) {
		visit(expr.object);
		return Type.unknown();
	}

	@Override
	public Type visitSetExpr(Expr.Set expr) {
		visit(expr.object);
		Type v = visit(expr.value);
		return v;
	}

	@Override
	public Type visitIndexExpr(Expr.Index expr) {
		Type arr = visit(expr.array);
		Type idx = visit(expr.index);
		if (arr.kind != Type.Kind.ARRAY) {
			RainLang.error(getLine(expr.array), "Indexing requires an array, got " + arr + ".");
			return Type.unknown();
		}
		requireVal(expr.index, idx, "Array index must be Val, got " + idx + ".");
		return arr.element;
	}

	@Override
	public Type visitIndexSetExpr(Expr.IndexSet expr) {
		Type arr = visit(expr.array);
		Type idx = visit(expr.index);
		Type val = visit(expr.value);
		if (arr.kind != Type.Kind.ARRAY) {
			RainLang.error(getLine(expr.array), "Index assignment requires an array, got " + arr + ".");
			return Type.unknown();
		}
		requireVal(expr.index, idx, "Array index must be Val, got " + idx + ".");
		if (!isAssignable(val, arr.element)) {
			RainLang.error(getLine(expr.value), "Cannot assign " + val + " into array of " + arr.element + ".");
		}
		return arr.element;
	}

	@Override
	public Type visitArrayExpr(Expr.Array expr) {
		if (expr.elements.isEmpty()) {
			return Type.arrayOf(Type.unknown());
		}
		Type first = visit(expr.elements.get(0));
		for (int i = 1; i < expr.elements.size(); i++) {
			Type t = visit(expr.elements.get(i));
			if (!first.equals(t)) {
				RainLang.error(getLine(expr.elements.get(i)),
					"Array elements must have uniform type; found " + first + " and " + t + ".");
			}
		}
		return Type.arrayOf(first);
	}

	@Override
	public Type visitThisExpr(Expr.This expr) {
		if (classStack.isEmpty()) {
			RainLang.error(expr.keyword.line, "Cannot use 'this' outside of a class.");
			return Type.unknown();
		}
		return classStack.peek();
	}

	// ---- Helpers ----
	private Type resolveTypeNode(Stmt.TypeNode node) {
		if (node.isNone) return Type.none();
		String n = node.name.lexeme;
		Type base;
		switch (n) {
			case "Val": base = Type.val(); break;
			case "Volume": base = Type.volume(); break;
			case "Area": base = Type.area(); break;
			case "Rain": base = Type.rain(); break;
			case "String": base = Type.string(); break;
			case "Bool": base = Type.bool(); break;
			default: base = Type.classType(n); break; // custom classes
		}
		return node.isArray ? Type.arrayOf(base) : base;
	}

	private boolean isAssignable(Type from, Type to) {
		if (to.equals(Type.unknown()) || from.equals(Type.unknown())) return true;
		if (from.equals(Type.none())) return true; // allow None to any (nullable)
		if (to.kind == Type.Kind.ARRAY) {
			return from.kind == Type.Kind.ARRAY && isAssignable(from.element, to.element);
		}
		if (to.kind == Type.Kind.CLASS) {
			return from.kind == Type.Kind.CLASS && Objects.equals(from.name, to.name);
		}
		return from.equals(to);
	}

	private void requireBool(Expr node, Type t, String msg) {
		if (!t.equals(Type.bool())) RainLang.error(getLine(node), msg);
	}
	private void requireVal(Expr node, Type t, String msg) {
		if (!t.equals(Type.val())) RainLang.error(getLine(node), msg);
	}
	private void requireNumeric(Expr node, Type t, String msg) {
		if (!t.isNumericDomain()) RainLang.error(getLine(node), msg);
	}

	private int getLine(Expr node) {
		// Best-effort line reporting for composite nodes
		if (node instanceof Expr.Variable) return ((Expr.Variable)node).name.line;
		if (node instanceof Expr.Literal) {
			Expr.Literal l = (Expr.Literal)node;
			return l.unit != null ? l.unit.line : 0;
		}
		if (node instanceof Expr.Unary) return ((Expr.Unary)node).operator.line;
		if (node instanceof Expr.Binary) return ((Expr.Binary)node).operator.line;
		if (node instanceof Expr.Logical) return ((Expr.Logical)node).operator.line;
		if (node instanceof Expr.Call) return ((Expr.Call)node).paren.line;
		if (node instanceof Expr.Get) return ((Expr.Get)node).name.line;
		if (node instanceof Expr.Index) return getLine(((Expr.Index)node).array);
		if (node instanceof Expr.IndexSet) return getLine(((Expr.IndexSet)node).array);
		if (node instanceof Expr.Grouping) return getLine(((Expr.Grouping)node).expression);
		if (node instanceof Expr.Array) return 0;
		if (node instanceof Expr.This) return ((Expr.This)node).keyword.line;
		return 0;
	}

	private Type binaryResult(Token op, Type L, Type R) {
		switch (op.type) {
			// Equality and inequalities
			case TokenType.EQUAL_EQUAL:
			case TokenType.BANG_EQUAL:
				return Type.bool();
			case TokenType.GREATER:
			case TokenType.GREATER_EQUAL:
			case TokenType.LESS:
			case TokenType.LESS_EQUAL:
				if (!L.isNumericDomain() || !R.isNumericDomain() || !L.equals(R)) {
					RainLang.error(op.line, "Relational operators require matching numeric types; got " + L + " and " + R + ".");
				}
				return Type.bool();

			// Addition
			case TokenType.PLUS:
				// String + (Val|Volume|Area|Rain|String) -> String
				if (L.equals(Type.string())) {
					if (R.equals(Type.string()) || R.isNumericDomain()) return Type.string();
					RainLang.error(op.line, "String '+' only supports String or numeric domains on RHS; got " + R + ".");
					return Type.string();
				}
				// Ensure arrays are of the same type
				if (L.kind == Type.Kind.ARRAY && R.kind == Type.Kind.ARRAY) {
					if (!L.element.equals(R.element)) {
						RainLang.error(op.line, "Array '+' requires same element type; got " + L + " and " + R + ".");
					}
					return Type.arrayOf(L.element);
				}
				// Volume/Area/Rain/Val same-kind addition
				if (L.equals(Type.volume()) && R.equals(Type.volume())) return Type.volume();
				if (L.equals(Type.area())  && R.equals(Type.area()))  return Type.area();
				if (L.equals(Type.rain())  && R.equals(Type.rain()))  return Type.rain();
				if (L.equals(Type.val())   && R.equals(Type.val()))   return Type.val();
				RainLang.error(op.line, "Invalid operator '+' between " + L + " and " + R + ".");
				return Type.unknown();

			// Subtraction mirrors same-kind numeric domains
			case TokenType.MINUS:
				if (L.equals(Type.volume()) && R.equals(Type.volume())) return Type.volume();
				if (L.equals(Type.area())  && R.equals(Type.area()))  return Type.area();
				if (L.equals(Type.rain())  && R.equals(Type.rain()))  return Type.rain();
				if (L.equals(Type.val())   && R.equals(Type.val()))   return Type.val();
				RainLang.error(op.line, "Invalid operator '-' between " + L + " and " + R + ".");
				return Type.unknown();

			// Multiplication
			case TokenType.STAR:
				// Val * X or X * Val -> X (X in Val, Volume, Area, Rain)
				if (L.equals(Type.val()) && R.isNumericDomain()) return R;
				if (R.equals(Type.val()) && L.isNumericDomain()) return L;
				// Area * Rain / Rain * Area -> Volume
				if ((L.equals(Type.area()) && R.equals(Type.rain())) || (L.equals(Type.rain()) && R.equals(Type.area())))
					return Type.volume();
				RainLang.error(op.line, "Invalid operator '*' between " + L + " and " + R + ".");
				return Type.unknown();

			// Division
			case TokenType.SLASH:
				// X / Val -> X
				if (R.equals(Type.val()) && L.isNumericDomain()) return L;
				// Volume / Rain -> Area
				if (L.equals(Type.volume()) && R.equals(Type.rain())) return Type.area();
				// Volume / Area -> Rain
				if (L.equals(Type.volume()) && R.equals(Type.area())) return Type.rain();
				RainLang.error(op.line, "Invalid operator '/' between " + L + " and " + R + ".");
				return Type.unknown();

			default:
				return Type.unknown();
		}
	}

	// Overloads are fun
	private Type visit(Expr e) { return e.accept(this); }
	private Void visit(Stmt s) { return s.accept(this); }
}