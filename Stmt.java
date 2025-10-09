import java.util.List;

abstract class Stmt {
	interface Visitor<R> {
		R visitExpressionStmt(Expression stmt);
		R visitVarDeclStmt(VarDecl stmt);
		R visitFunctionStmt(Function stmt);
		R visitClassStmt(ClassStmt stmt);
		R visitBlockStmt(Block stmt);
		R visitIfStmt(If stmt);
		R visitWhileStmt(While stmt);
		R visitForStmt(For stmt);
		R visitReturnStmt(Return stmt);
		R visitFieldStmt(Field stmt);
		R visitConstructorStmt(Constructor stmt);
	}

	static class Expression extends Stmt {
		final Expr expression;
		Expression(Expr expression) { this.expression = expression; }
		@Override <R> R accept(Visitor<R> visitor) { return visitor.visitExpressionStmt(this); }
	}

	static class VarDecl extends Stmt {
		final TypeNode type;
		final Token name;
		final Expr initializer;
		VarDecl(TypeNode type, Token name, Expr initializer) {
			this.type = type; this.name = name; this.initializer = initializer;
		}
		@Override <R> R accept(Visitor<R> visitor) { return visitor.visitVarDeclStmt(this); }
	}

	static class Function extends Stmt {
		final TypeNode returnType;
		final Token name;
		final List<Param> params;
		final List<Stmt> body;
		Function(TypeNode returnType, Token name, List<Param> params, List<Stmt> body) {
			this.returnType = returnType; this.name = name; this.params = params; this.body = body;
		}
		@Override <R> R accept(Visitor<R> visitor) { return visitor.visitFunctionStmt(this); }
	}

	static class ClassStmt extends Stmt {
		final Token name;
		final List<Stmt> members; // VarDecl or Function
		ClassStmt(Token name, List<Stmt> members) { this.name = name; this.members = members; }
		@Override <R> R accept(Visitor<R> visitor) { return visitor.visitClassStmt(this); }
	}

	static class Block extends Stmt {
		final List<Stmt> statements;
		Block(List<Stmt> statements) { this.statements = statements; }
		@Override <R> R accept(Visitor<R> visitor) { return visitor.visitBlockStmt(this); }
	}

	static class If extends Stmt {
		final Expr condition;
		final Stmt thenBranch;
		final Stmt elseBranch; // nullable
		If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
			this.condition = condition; this.thenBranch = thenBranch; this.elseBranch = elseBranch;
		}
		@Override <R> R accept(Visitor<R> visitor) { return visitor.visitIfStmt(this); }
	}

	static class While extends Stmt {
		final Expr condition;
		final Stmt body;
		While(Expr condition, Stmt body) { this.condition = condition; this.body = body; }
		@Override <R> R accept(Visitor<R> visitor) { return visitor.visitWhileStmt(this); }
	}

	static class For extends Stmt {
		final Stmt initializer; // VarDecl or Expression or null
		final Expr condition;	 // nullable
		final Expr increment;	 // nullable
		final Stmt body;
		For(Stmt initializer, Expr condition, Expr increment, Stmt body) {
			this.initializer = initializer; this.condition = condition; this.increment = increment; this.body = body;
		}
		@Override <R> R accept(Visitor<R> visitor) { return visitor.visitForStmt(this); }
	}

	static class Return extends Stmt {
		final Token keyword;
		final Expr value; // nullable
		Return(Token keyword, Expr value) { this.keyword = keyword; this.value = value; }
		@Override <R> R accept(Visitor<R> visitor) { return visitor.visitReturnStmt(this); }
	}

	abstract <R> R accept(Visitor<R> visitor);
	static class Param {
		final TypeNode type;
		final Token name;
		Param(TypeNode type, Token name) { this.type = type; this.name = name; }
	}

	static class TypeNode {
		final Token name;
		final boolean isNone;
		final boolean isArray;
		TypeNode(Token name, boolean isNone, boolean isArray) {
			this.name = name; this.isNone = isNone; this.isArray = isArray;
		}
		@Override public String toString() {
			if (isNone) return "None";
			return name.lexeme + (isArray ? "[]" : "");
		}
	}
	static class Field extends Stmt {
		final TypeNode type;
		final Token name;
		final Expr initializer; // may be null
		Field(TypeNode type, Token name, Expr initializer) {
			this.type = type;
			this.name = name;
			this.initializer = initializer;
		}
		@Override <R> R accept(Visitor<R> visitor) {
			return visitor.visitFieldStmt(this);
		}
	}
	static class Constructor extends Stmt {
		final Token name;
		final List<Param> params;
		final List<Stmt> body;
		Constructor(Token name, List<Param> params, List<Stmt> body) {
			this.name = name;
			this.params = params;
			this.body = body;
		}
		@Override <R> R accept(Visitor<R> visitor) {
			return visitor.visitConstructorStmt(this);
		}
	}
}