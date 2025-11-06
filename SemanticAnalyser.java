import java.util.*;

class SemanticAnalyser implements Expr.Visitor<Type>, Stmt.Visitor<Void> {
	private final TypeEnvironment env = new TypeEnvironment();
	private final Deque<Type> returnStack = new ArrayDeque<>();
	private final Deque<Type> classStack = new ArrayDeque<>();
	private boolean firstEnter = true;
	private int loopDepth = 0;

	// member typing registry
	private static final class FnSig {
		final Type ret; final List<Type> params;
		FnSig(Type ret, List<Type> params) { this.ret = ret; this.params = params; }
	}
	private static final class ClassInfo {
		final String name;
		final Map<String, Type> fields = new HashMap<>();
		final Map<String, FnSig> methods = new HashMap<>();
		FnSig ctor = null;
		ClassInfo(String name) { this.name = name; }
	}
	private final Map<String, ClassInfo> classes = new HashMap<>();

	void analyse(List<Stmt> program) {
		if (firstEnter) {
			Builtins.registerTypes(env);
			firstEnter = false;
		}
		for (Stmt s : program) visit(s);
	}

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
		Type retType = resolveTypeNode(stmt.returnType);
		List<Type> paramTypes = new ArrayList<>(stmt.params.size());
		for (Stmt.Param p : stmt.params) paramTypes.add(resolveTypeNode(p.type));
		Type fnType = Type.function(retType, paramTypes);
		env.addFunctionOverload(stmt.name.lexeme, fnType);

		env.push();
		returnStack.push(retType);
		for (int i = 0; i < stmt.params.size(); i++) {
			Stmt.Param p = stmt.params.get(i);
			env.define(p.name.lexeme, paramTypes.get(i));
		}
		for (Stmt s : stmt.body) visit(s);
		returnStack.pop();
		env.pop();

		if (!classStack.isEmpty()) {
			String cname = classStack.peek().name;
			ClassInfo ci = classes.get(cname);
			if (ci != null) ci.methods.put(stmt.name.lexeme, new FnSig(retType, paramTypes));
		}
		return null;
	}

	@Override
	public Void visitClassStmt(Stmt.ClassStmt stmt) {
		Type cls = Type.classType(stmt.name.lexeme);
		env.define(stmt.name.lexeme, cls);

		classes.put(stmt.name.lexeme, new ClassInfo(stmt.name.lexeme));

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
		loopDepth++;
		visit(stmt.body);
		loopDepth--;
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
		loopDepth++;
		visit(stmt.body);
		loopDepth--;
		env.pop();
		return null;
	}

	@Override
	public Void visitBreakStmt(Stmt.Break stmt) {
		if (loopDepth == 0) {
			RainLang.error(stmt.keyword.line, "break used outside of a loop.");
		}
		return null;
	}

	@Override
	public Void visitContinueStmt(Stmt.Continue stmt) {
		if (loopDepth == 0) {
			RainLang.error(stmt.keyword.line, "continue used outside of a loop.");
		}
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

		if (!classStack.isEmpty()) {
			ClassInfo ci = classes.get(classStack.peek().name);
			if (ci != null) ci.fields.put(stmt.name.lexeme, declared);
		}

		if (stmt.initializer != null) {
			Type init = visit(stmt.initializer);
			if (!isAssignable(init, declared)) {
				RainLang.error(stmt.type.name.line,
					"Cannot assign " + init + " to field '" + stmt.name.lexeme + "' of type " + declared + ".");
			}
		}
		env.define(stmt.name.lexeme, declared);
		return null;
	}

	@Override
	public Void visitConstructorStmt(Stmt.Constructor stmt) {
		if (!classStack.isEmpty()) {
			String cname = classStack.peek().name;
			ClassInfo ci = classes.get(cname);
			if (ci != null) {
				List<Type> paramTypes = new ArrayList<>(stmt.params.size());
				for (Stmt.Param p : stmt.params) paramTypes.add(resolveTypeNode(p.type));
				ci.ctor = new FnSig(Type.classType(cname), paramTypes);
			}
		}

		env.push();
		for (Stmt.Param p : stmt.params) {
			Type pt = resolveTypeNode(p.type);
			env.define(p.name.lexeme, pt);
		}
		for (Stmt s : stmt.body) visit(s);
		env.pop();
		return null;
	}

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
		// Free function overloads or constructor calls when callee is an identifier
		if (expr.callee instanceof Expr.Variable v) {
			String fname = v.name.lexeme;

			List<Type> argTypes = new ArrayList<>(expr.arguments.size());
			for (Expr a : expr.arguments) argTypes.add(visit(a));

			// Try free-function overloads first
			List<Type> candidates = env.lookupFunctionOverloads(fname);
			if (candidates != null && !candidates.isEmpty()) {
				List<Integer> matches = new ArrayList<>();
				for (int i = 0; i < candidates.size(); i++) {
					Type cand = candidates.get(i);
					if (cand.kind != Type.Kind.FUNCTION) continue;
					List<Type> params = cand.paramTypes;
					if (params.size() != argTypes.size()) continue;
					boolean ok = true;
					for (int j = 0; j < params.size(); j++) {
						if (!isAssignable(argTypes.get(j), params.get(j))) { ok = false; break; }
					}
					if (ok) matches.add(i);
				}
				if (matches.size() == 1) {
					int slot = matches.get(0);
					Type chosen = candidates.get(slot);
					CallResolution.bind(expr, fname, slot);
					return chosen.returnType;
				}
				if (matches.isEmpty()) {
					RainLang.error(getLine(expr), "No overload of '" + fname + "' matches argument types (" +
							String.join(", ", argTypes.stream().map(Type::toString).toList()) + ").");
					return Type.unknown();
				}
				RainLang.error(getLine(expr), "Ambiguous call to '" + fname + "' with argument types (" +
						String.join(", ", argTypes.stream().map(Type::toString).toList()) + ").");
				return Type.unknown();
			}

			// Fall back to constructor call if the identifier is a class
			Type sym = env.lookup(fname);
			if (sym != null && sym.kind == Type.Kind.CLASS) {
				ClassInfo ci = classes.get(sym.name);
				if (ci != null && ci.ctor != null) {
					List<Type> params = ci.ctor.params;
					if (params.size() != argTypes.size()) {
						RainLang.error(getLine(expr), "Expected " + params.size() + " arguments but got " + argTypes.size() + ".");
					} else {
						for (int i = 0; i < params.size(); i++) {
							if (!isAssignable(argTypes.get(i), params.get(i))) {
								RainLang.error(getLine(expr.arguments.get(i)),
									"Argument " + (i + 1) + " type mismatch: expected " + params.get(i) + " but got " + argTypes.get(i) + ".");
							}
						}
					}
				} else if (!argTypes.isEmpty()) {
					RainLang.error(getLine(expr), "Expected 0 arguments but got " + argTypes.size() + ".");
				}
				return sym;
			}
		}

		// General callable (methods/array built-ins/etc.)
		Type calleeT = visit(expr.callee);

		List<Type> argTypes = new ArrayList<>(expr.arguments.size());
		for (Expr a : expr.arguments) argTypes.add(visit(a));

		if (calleeT.kind == Type.Kind.FUNCTION) {
			List<Type> params = calleeT.paramTypes;
			if (params.size() != argTypes.size()) {
				RainLang.error(getLine(expr), "Expected " + params.size() + " arguments but got " + argTypes.size() + ".");
			} else {
				for (int i = 0; i < params.size(); i++) {
					if (!isAssignable(argTypes.get(i), params.get(i))) {
						RainLang.error(getLine(expr.arguments.get(i)),
							"Argument " + (i + 1) + " type mismatch: expected " + params.get(i) + " but got " + argTypes.get(i) + ".");
					}
				}
			}
			return calleeT.returnType;
		}

		RainLang.error(getLine(expr.callee), "Attempted to call a non-callable expression of type " + calleeT + ".");
		return Type.unknown();
	}


	@Override
	public Type visitGetExpr(Expr.Get expr) {
		Type recv = visit(expr.object);

		if (recv.kind == Type.Kind.ARRAY) {
			String m = expr.name.lexeme;
			if ("length".equals(m)) return Type.val();
			FnSig sig = arrayMethodSig(m, recv.element);
			if (sig != null) return Type.function(sig.ret, sig.params);
			RainLang.error(expr.name.line, "Unknown member '" + m + "' on array of " + recv.element + ".");
			return Type.unknown();
		}

		if (recv.equals(Type.string())) {
			String m = expr.name.lexeme;
			if ("length".equals(m)) return Type.val();
			FnSig sig = stringMethodSig(m);
			if (sig != null) return Type.function(sig.ret, sig.params);
			RainLang.error(expr.name.line, "Unknown member '" + m + "' on String.");
			return Type.unknown();
		}

		if (recv.kind != Type.Kind.CLASS) {
			RainLang.error(getLine(expr.object), "Property access requires a class instance, array, or string; got " + recv + ".");
			return Type.unknown();
		}
		ClassInfo ci = classes.get(recv.name);
		if (ci == null) {
			RainLang.error(getLine(expr.object), "Unknown class '" + recv.name + "'.");
			return Type.unknown();
		}
		Type f = ci.fields.get(expr.name.lexeme);
		if (f != null) return f;
		FnSig m = ci.methods.get(expr.name.lexeme);
		if (m != null) return Type.function(m.ret, m.params);

		RainLang.error(expr.name.line, "Unknown member '" + expr.name.lexeme + "' on class " + recv.name + ".");
		return Type.unknown();
	}

	@Override
	public Type visitSetExpr(Expr.Set expr) {
		Type recv = visit(expr.object);
		if (recv.kind != Type.Kind.CLASS) {
			RainLang.error(getLine(expr.object), "Field assignment requires a class instance, got " + recv + ".");
			visit(expr.value);
			return Type.unknown();
		}
		ClassInfo ci = classes.get(recv.name);
		if (ci == null) {
			RainLang.error(getLine(expr.object), "Unknown class '" + recv.name + "'.");
			visit(expr.value);
			return Type.unknown();
		}
		if (ci.methods.containsKey(expr.name.lexeme)) {
			RainLang.error(expr.name.line, "Cannot assign to method '" + expr.name.lexeme + "'.");
			visit(expr.value);
			return Type.unknown();
		}
		Type fieldT = ci.fields.get(expr.name.lexeme);
		if (fieldT == null) {
			RainLang.error(expr.name.line, "Unknown field '" + expr.name.lexeme + "' on class " + recv.name + ".");
			visit(expr.value);
			return Type.unknown();
		}
		Type v = visit(expr.value);
		if (!isAssignable(v, fieldT)) {
			RainLang.error(getLine(expr.value), "Cannot assign " + v + " to field '" + expr.name.lexeme + "' of type " + fieldT + ".");
		}
		return fieldT;
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
			default: base = Type.classType(n); break;
		}
		return node.isArray ? Type.arrayOf(base) : base;
	}

	private boolean isAssignable(Type from, Type to) {
		if (to.equals(Type.unknown()) || from.equals(Type.unknown())) return true;
		if (from.equals(Type.none())) return true;
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

			case TokenType.PLUS:
				if (L.equals(Type.string())) {
					if (R.equals(Type.string()) || R.isNumericDomain()) return Type.string();
					RainLang.error(op.line, "String '+' only supports String or numeric domains on RHS; got " + R + ".");
					return Type.string();
				}
				if (L.kind == Type.Kind.ARRAY && R.kind == Type.Kind.ARRAY) {
					if (!L.element.equals(R.element)) {
						RainLang.error(op.line, "Array '+' requires same element type; got " + L + " and " + R + ".");
					}
					return Type.arrayOf(L.element);
				}
				if (L.equals(Type.volume()) && R.equals(Type.volume())) return Type.volume();
				if (L.equals(Type.area())  && R.equals(Type.area()))  return Type.area();
				if (L.equals(Type.rain())  && R.equals(Type.rain()))  return Type.rain();
				if (L.equals(Type.val())   && R.equals(Type.val()))   return Type.val();
				RainLang.error(op.line, "Invalid operator '+' between " + L + " and " + R + ".");
				return Type.unknown();

			case TokenType.MINUS:
				if (L.equals(Type.volume()) && R.equals(Type.volume())) return Type.volume();
				if (L.equals(Type.area())  && R.equals(Type.area()))  return Type.area();
				if (L.equals(Type.rain())  && R.equals(Type.rain()))  return Type.rain();
				if (L.equals(Type.val())   && R.equals(Type.val()))   return Type.val();
				RainLang.error(op.line, "Invalid operator '-' between " + L + " and " + R + ".");
				return Type.unknown();

			case TokenType.STAR:
				if (L.equals(Type.val()) && R.isNumericDomain()) return R;
				if (R.equals(Type.val()) && L.isNumericDomain()) return L;
				if ((L.equals(Type.area()) && R.equals(Type.rain())) || (L.equals(Type.rain()) && R.equals(Type.area())))
					return Type.volume();
				RainLang.error(op.line, "Invalid operator '*' between " + L + " and " + R + ".");
				return Type.unknown();

			case TokenType.SLASH:
				if (R.equals(Type.val()) && L.isNumericDomain()) return L;
				if (L.equals(Type.volume()) && R.equals(Type.rain())) return Type.area();
				if (L.equals(Type.volume()) && R.equals(Type.area())) return Type.rain();
				RainLang.error(op.line, "Invalid operator '/' between " + L + " and " + R + ".");
				return Type.unknown();

			default:
				return Type.unknown();
		}
	}

	private FnSig arrayMethodSig(String name, Type elem) {
		switch (name) {
			case "push":     return new FnSig(Type.val(), Arrays.asList(elem));
			case "pop":      return new FnSig(elem,      Collections.emptyList());
			case "clear":    return new FnSig(Type.none(), Collections.emptyList());
			case "insert":   return new FnSig(Type.none(), Arrays.asList(Type.val(), elem));
			case "removeAt": return new FnSig(elem,      Arrays.asList(Type.val()));
			default:         return null;
		}
	}

	private FnSig stringMethodSig(String name) {
		return null;
	}

	private Type visit(Expr e) { return e.accept(this); }
	private Void visit(Stmt s) { return s.accept(this); }
}
