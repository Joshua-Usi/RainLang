import java.util.*;

class Parser {
	private final List<Token> tokens;
	private int current = 0;

	// Non-throwing error handling state
	// Suppress cascaded reports during recovery. Returns to false when we reached a statement boundary
	private boolean panicMode = false;
	private boolean hadError  = false;

	Parser(List<Token> tokens) { this.tokens = tokens; }

	// program → statement* EOF
	List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<>();
		while (!isAtEnd()) {
			Stmt s = declaration();
			if (s != null) statements.add(s);
		}
		return statements;
	}

	// declaration → (variable_decl ";") | function_decl | class_decl | statement
	private Stmt declaration() {
		if (match(TokenType.CLASS)) return classDecl();

		// Prefer explicit declarators before falling back to statement/expr
		if (startsFunctionDecl()) {
			Stmt fn = functionDecl();
			if (fn != null) return fn;
			synchronise(); return null;
		}
		if (startsVarDecl()) {
			Stmt vd = variableDecl();
			if (vd != null) {
				need(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
				return vd;
			}
			synchronise(); return null;
		}

		// Fallback: generic statement
		Stmt st = statement();
		if (st == null) synchronise();
		return st;
	}

	// class_decl       → "class" identifier "{" class_member* "}"
	// class_member     → function_decl | variable_decl ";"
	// field_decl       → type identifier ("=" expression)? ";" ;
	// constructor_decl → identifier "(" param_list? ")" block ;
	private Stmt classDecl() {
		Token name = need(TokenType.IDENTIFIER, "Expect class name.");
		if (name == null) { synchronise(); return null; }
		if (need(TokenType.LEFT_BRACE, "Expect '{' before class body.") == null) { synchronise(); return null; }
		List<Stmt> members = new ArrayList<>();
		while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			// Identify Constructor
			if (check(TokenType.IDENTIFIER) && peek().lexeme.equals(name.lexeme) && peek(1).type == TokenType.LEFT_PAREN) {
				// Class name
				Token ctorName = advance();
				if (need(TokenType.LEFT_PAREN, "Expect '(' after constructor name.") == null) { synchronise(); continue; }

				List<Stmt.Param> params = new ArrayList<>();
				if (!check(TokenType.RIGHT_PAREN)) {
					// Ew do while
					do {
						Stmt.TypeNode pt = type();
						Token pn = need(TokenType.IDENTIFIER, "Expect parameter name.");
						if (pt == null || pn == null) { synchronise(); break; }
						params.add(new Stmt.Param(pt, pn));
					} while (match(TokenType.COMMA));
				}
				if (need(TokenType.RIGHT_PAREN, "Expect ')' after parameters.") == null) { synchronise(); continue; }
				if (need(TokenType.LEFT_BRACE, "Expect '{' before constructor body.") == null) { synchronise(); continue; }
				List<Stmt> body = block();

				members.add(new Stmt.Constructor(ctorName, params, body));
				continue;
			}

			// Otherwise its a method or a field
			if (!isTypeHead()) {
				report(peek(), "Expect type for class member.");
				synchronise();
				continue;
			}

			Stmt.TypeNode t = type();
			Token memberName = need(TokenType.IDENTIFIER, "Expect member name.");
			if (memberName == null) { synchronise(); continue; }

			if (match(TokenType.LEFT_PAREN)) {
				// Method
				List<Stmt.Param> params = new ArrayList<>();
				if (!check(TokenType.RIGHT_PAREN)) {
					do {
						Stmt.TypeNode pt = type();
						Token pn = need(TokenType.IDENTIFIER, "Expect parameter name.");
						if (pt == null || pn == null) { synchronise(); break; }
						params.add(new Stmt.Param(pt, pn));
					} while (match(TokenType.COMMA));
				}

				if (need(TokenType.RIGHT_PAREN, "Expect ')' after parameters.") == null) { synchronise(); continue; }
				if (need(TokenType.LEFT_BRACE, "Expect '{' before method body.") == null) { synchronise(); continue; }
				List<Stmt> body = block();

				members.add(new Stmt.Function(t, memberName, params, body));
			} else {
				// Optional initialiser
				Expr init = null;
				if (match(TokenType.EQUAL)) init = expression();
				if (need(TokenType.SEMICOLON, "Expect ';' after field declaration.") == null) { synchronise(); continue; }
				members.add(new Stmt.Field(t, memberName, init));
			}
		}
		if (need(TokenType.RIGHT_BRACE, "Expect '}' after class body.") == null) synchronise();
		return new Stmt.ClassStmt(name, members);
	}


	// function_decl → type identifier "(" param_list? ")" block
	private Stmt functionDecl() {
		Stmt.TypeNode returnType = type();
		if (returnType == null) { synchronise(); return null; }

		Token name = need(TokenType.IDENTIFIER, "Expect function name.");
		if (name == null) { synchronise(); return null; }

		if (need(TokenType.LEFT_PAREN, "Expect '(' after function name.") == null) { synchronise(); return null; }

		List<Stmt.Param> params = new ArrayList<>();
		if (!check(TokenType.RIGHT_PAREN)) {
			// Do while look so ugly but they actually work so well
			do {
				Stmt.TypeNode pt = type();
				if (pt == null) { synchronise(); return null; }
				Token pn = need(TokenType.IDENTIFIER, "Expect parameter name.");
				if (pn == null) { synchronise(); return null; }
				params.add(new Stmt.Param(pt, pn));
			} while (match(TokenType.COMMA));
		}
		if (need(TokenType.RIGHT_PAREN, "Expect ')' after parameters.") == null) { synchronise(); return null; }
		if (need(TokenType.LEFT_BRACE, "Expect '{' before function body.") == null) { synchronise(); return null; }

		List<Stmt> body = block();
		return new Stmt.Function(returnType, name, params, body);
	}

	// variable_decl → type identifier "=" expression
	private Stmt variableDecl() {
		Stmt.TypeNode t = type();
		if (t == null) return null;

		Token name = need(TokenType.IDENTIFIER, "Expect variable name.");
		if (name == null) return null;

		if (need(TokenType.EQUAL, "Expect '=' after variable name.") == null) return null;

		Expr initializer = expression();
		if (initializer == null) initializer = new Expr.Literal(null, null);
		return new Stmt.VarDecl(t, name, initializer);
	}

	private Stmt.TypeNode type() {
		if (match(TokenType.NONE)) {
			return new Stmt.TypeNode(previous(), true, 0);
		}
		if (!match(TokenType.IDENTIFIER)) {
			report(peek(), "Expect type name.");
			return null;
		}
		Token name = previous();

		// Support N-dimensional arrays: T[], T[][], T[][][], ...
		int arrayDepth = 0;
		while (match(TokenType.LEFT_BRACKET)) {
			if (need(TokenType.RIGHT_BRACKET, "Expect ']' after '[' in array type.") == null) {
				// Treat as an array dimension anyway so later phases don't explode
			}
			arrayDepth++;
		}
		return new Stmt.TypeNode(name, false, arrayDepth);
	}

	// statement → return_stmt | control_flow | block | expression_stmt | continue | break
		private Stmt statement() {
		if (match(TokenType.RETURN)) return returnStmt();
		if (match(TokenType.IF))         return ifStmt();
		if (match(TokenType.WHILE)) return whileStmt();
		if (match(TokenType.FOR))       return forStmt();
		if (match(TokenType.BREAK))     return breakStmt();
		if (match(TokenType.CONTINUE))  return continueStmt();
		if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());
		return expressionStmt();
	}

	private Stmt breakStmt() {
		Token kw = previous();
		need(TokenType.SEMICOLON, "Expect ';' after break.");
		return new Stmt.Break(kw);
	}

	private Stmt continueStmt() {
		Token kw = previous();
		need(TokenType.SEMICOLON, "Expect ';' after continue.");
		return new Stmt.Continue(kw);
	}

	// return_stmt → "return" expression? ";"
	private Stmt returnStmt() {
		Token kw = previous();
		Expr value = null;
		if (!check(TokenType.SEMICOLON)) value = expression();
		need(TokenType.SEMICOLON, "Expect ';' after return.");
		return new Stmt.Return(kw, value);
	}

	// if_stmt → "if" "(" expression ")" block ( "else" ( if_stmt | block ) )?
	private Stmt ifStmt() {
		if (need(TokenType.LEFT_PAREN, "Expect '(' after 'if'.") == null) { synchronise(); return null; }
		Expr cond = expression();
		if (need(TokenType.RIGHT_PAREN, "Expect ')' after if condition.") == null) { synchronise(); return null; }
		Stmt thenBranch = statement();
		// Recursive for if else branches
		Stmt elseBranch = null;
		if (match(TokenType.ELSE)) {
			elseBranch = (match(TokenType.IF)) ? ifStmt() : statement();
		}
		return new Stmt.If(cond, thenBranch, elseBranch);
	}

	// while_stmt   → WHILE "(" expression ")" block ;
	private Stmt whileStmt() {
		if (need(TokenType.LEFT_PAREN, "Expect '(' after 'while'.") == null) { synchronise(); return null; }
		Expr cond = expression();
		if (need(TokenType.RIGHT_PAREN, "Expect ')' after while condition.") == null) { synchronise(); return null; }
		Stmt body = statement();
		return new Stmt.While(cond, body);
	}

	// for_stmt → "for" "(" (variable_decl | expression)? ";" expression? ";" expression? ")" block
	private Stmt forStmt() {
		if (need(TokenType.LEFT_PAREN, "Expect '(' after 'for'.") == null) { synchronise(); return null; }

		Stmt initializer = null;
		if (!check(TokenType.SEMICOLON)) {
			if (isTypeHead()) {
				initializer = variableDecl();
				need(TokenType.SEMICOLON, "Expect ';' after for initializer.");
			} else {
				Expr initExpr = expression();
				need(TokenType.SEMICOLON, "Expect ';' after for initializer.");
				initializer = new Stmt.Expression(initExpr);
			}
		} else {
			// consume ';'
			advance();
		}

		Expr condition = null;
		if (!check(TokenType.SEMICOLON)) condition = expression();
		need(TokenType.SEMICOLON, "Expect ';' after for condition.");

		Expr increment = null;
		if (!check(TokenType.RIGHT_PAREN)) increment = expression();
		if (need(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.") == null) { synchronise(); return null; }

		Stmt body = statement();
		return new Stmt.For(initializer, condition, increment, body);
	}

	// block → "{" declaration* "}"
	private List<Stmt> block() {
		List<Stmt> stmts = new ArrayList<>();
		while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			Stmt d = declaration();
			if (d != null) stmts.add(d);
			else synchronise();
		}
		need(TokenType.RIGHT_BRACE, "Expect '}' after block.");
		return stmts;
	}

	// expression_stmt → expression ";"
	private Stmt expressionStmt() {
		Expr expr = expression();
		if (need(TokenType.SEMICOLON, "Expect ';' after expression.") == null) {
			synchronise();
			return null;
		}
		return new Stmt.Expression(expr);
	}

	private Expr expression() { return assignment(); }

	// assignment → lvalue "=" assignment | logical_or
	private Expr assignment() {
		Expr left = logicalOr();
		if (match(TokenType.EQUAL)) {
			Expr value = assignment();

			if (left instanceof Expr.Variable v) {
				return new Expr.Assign(v.name, value);
			} else if (left instanceof Expr.Get g) {
				return new Expr.Set(g.object, g.name, value);
			} else if (left instanceof Expr.Index idx) {
				return new Expr.IndexSet(idx.array, idx.index, value, idx.bracket);
			}
			// drop target; keep RHS to continue
			report(previous(), "Invalid assignment target.");
			return value;
		}
		return left;
	}

	// logical_or → logical_and ( "||" logical_and )*
	private Expr logicalOr() {
		Expr expr = logicalAnd();
		while (match(TokenType.OR_OR)) {
			Token op = previous();
			Expr right = logicalAnd();
			expr = new Expr.Logical(expr, op, right);
		}
		return expr;
	}

	// logical_and → equality ( "&&" equality )*
	private Expr logicalAnd() {
		Expr expr = equality();
		while (match(TokenType.AND_AND)) {
			Token op = previous();
			Expr right = equality();
			expr = new Expr.Logical(expr, op, right);
		}
		return expr;
	}

	// equality → relational ( ("==" | "!=") relational )?
	private Expr equality() {
		Expr expr = relational();
		if (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
			Token op = previous();
			Expr right = relational();
			expr = new Expr.Binary(expr, op, right);
		}
		return expr;
	}

	// relational → additive ( ("<" | "<=" | ">" | ">=") additive )?
	private Expr relational() {
		Expr expr = additive();
		if (match(TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
			Token op = previous();
			Expr right = additive();
			expr = new Expr.Binary(expr, op, right);
		}
		return expr;
	}

	// additive → multiplicative ( ("+" | "-") multiplicative )*
	private Expr additive() {
		Expr expr = multiplicative();
		while (match(TokenType.PLUS, TokenType.MINUS)) {
			Token op = previous();
			Expr right = multiplicative();
			expr = new Expr.Binary(expr, op, right);
		}
		return expr;
	}

	// multiplicative → unary ( ("*" | "/") unary )*
	private Expr multiplicative() {
		Expr expr = unary();
		while (match(TokenType.STAR, TokenType.SLASH)) {
			Token op = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, op, right);
		}
		return expr;
	}

	// unary → ( "!" | "+" | "-" ) unary | postfix
	private Expr unary() {
		if (match(TokenType.BANG, TokenType.PLUS, TokenType.MINUS)) {
			Token op = previous();
			Expr right = unary();
			return new Expr.Unary(op, right);
		}
		return postfix();
	}

	// postfix → primary ( "(" args? ")" | "[" expression "]" | "." identifier )*
	private Expr postfix() {
		Expr expr = primary();
		while (true) {
			if (match(TokenType.LEFT_PAREN)) {
				expr = finishCall(expr);
			} else if (match(TokenType.LEFT_BRACKET)) {
				Expr index = expression();
				need(TokenType.RIGHT_BRACKET, "Expect ']' after index.");
				Token bracket = previousIfType(TokenType.RIGHT_BRACKET);
				if (bracket == null) bracket = synthetic(TokenType.RIGHT_BRACKET, "]");
				expr = new Expr.Index(expr, index, bracket);
			} else if (match(TokenType.DOT)) {
				Token name = need(TokenType.IDENTIFIER, "Expect property name after '.'.");
				if (name == null) name = syntheticIdent("<missing>");
				expr = new Expr.Get(expr, name);
			} else {
				break;
			}
		}
		return expr;
	}

	private Expr finishCall(Expr callee) {
		List<Expr> args = new ArrayList<>();
		if (!check(TokenType.RIGHT_PAREN)) {
			do { args.add(expression()); } while (match(TokenType.COMMA));
		}
		need(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
		Token paren = previousIfType(TokenType.RIGHT_PAREN);
		if (paren == null) paren = synthetic(TokenType.RIGHT_PAREN, ")");
		return new Expr.Call(callee, paren, args);
	}

	// primary → literal | identifier | "this" | "(" expression ")"
	private Expr primary() {
		if (match(TokenType.NUMBER)) {
			Token num = previous();
			Token unit = null;
			// Unit suffixes
			if (match(TokenType.IDENTIFIER)) unit = previous();
			return new Expr.Literal(num.literal, unit);
		}
		if (match(TokenType.STRING)) return new Expr.Literal(previous().literal, null);
		if (match(TokenType.TRUE))   return new Expr.Literal(Boolean.TRUE, null);
		if (match(TokenType.FALSE))  return new Expr.Literal(Boolean.FALSE, null);
		if (match(TokenType.NONE))   return new Expr.Literal(null, null);
		if (match(TokenType.THIS))   return new Expr.This(previous());

		if (match(TokenType.IDENTIFIER)) return new Expr.Variable(previous());

		if (match(TokenType.LEFT_BRACKET)) {
			List<Expr> elements = new ArrayList<>();
			if (!check(TokenType.RIGHT_BRACKET)) {
				do { elements.add(expression()); } while (match(TokenType.COMMA));
			}
			need(TokenType.RIGHT_BRACKET, "Expect ']' after array elements.");
			return new Expr.Array(elements);
		}

		if (match(TokenType.LEFT_PAREN)) {
			Expr expr = expression();
			need(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}

		report(peek(), "Expected expression.");
		// Placeholder to keep parsing
		return new Expr.Literal(null, null);
	}
	private boolean isTypeHead() {
		return check(TokenType.NONE) || check(TokenType.IDENTIFIER);
	}

	private boolean startsFunctionDecl() {
		int i = 0;
		boolean idType = (peek(i).type == TokenType.IDENTIFIER);
		if (!(idType || peek(i).type == TokenType.NONE)) return false;
		i++;

		// Only IDENTIFIER types can be arrays; consume all [] pairs
		if (idType) {
			while (peek(i).type == TokenType.LEFT_BRACKET && peek(i + 1).type == TokenType.RIGHT_BRACKET) {
				i += 2;
			}
		}

		return (peek(i).type == TokenType.IDENTIFIER && peek(i + 1).type == TokenType.LEFT_PAREN);
	}

	private boolean startsVarDecl() {
		int i = 0;
		boolean idType = (peek(i).type == TokenType.IDENTIFIER);
		if (!(idType || peek(i).type == TokenType.NONE)) return false;
		i++;

		// Only IDENTIFIER types can be arrays; consume all [] pairs
		if (idType) {
			while (peek(i).type == TokenType.LEFT_BRACKET && peek(i + 1).type == TokenType.RIGHT_BRACKET) {
				i += 2;
			}
		}

		return (peek(i).type == TokenType.IDENTIFIER && peek(i + 1).type == TokenType.EQUAL);
	}
	
	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) { advance(); return true; }
		}
		return false;
	}

	private boolean check(TokenType type) {
		if (isAtEnd()) return false;
		return peek().type == type;
	}

	private Token advance() {
		if (!isAtEnd()) current++;
		return previous();
	}

	private boolean isAtEnd() { return peek().type == TokenType.EOF; }
	private Token peek(int lookahead) {
		// Safety, just get last token
		if (current + lookahead >= tokens.size()) return tokens.get(tokens.size() - 1);
		return tokens.get(current + lookahead);
	}
	private Token peek() { return tokens.get(current); }
	private Token previous() { return tokens.get(current - 1); }

	// Basically consume
	private Token need(TokenType type, String message) {
		if (check(type)) return advance();
		report(peek(), message);
		return null;
	}

	// report errors; guard with panicMode
	private void report(Token token, String message) {
		if (!panicMode) {
			hadError = true;
			RainLang.error(token.line, message);
		}
		panicMode = true;
	}

	// synchronise to next statement boundary, clear panicMode afterwards
	private void synchronise() {
		advance();
		while (!isAtEnd()) {
			if (previous().type == TokenType.SEMICOLON) break;
			switch (peek().type) {
				case RIGHT_BRACE:
				case CLASS:
				case RETURN:
				case IF:
				case WHILE:
				case FOR:
				case BREAK:
				case CONTINUE:
					break;
				default:
					advance();
					continue;
			}
			break;
		}
		panicMode = false;
	}

	private Token previousIfType(TokenType type) {
		return (previous().type == type) ? previous() : null;
	}

	// Produce a synthetic lexeme so we can keep parsing
	private Token synthetic(TokenType type, String lexeme) {
		return new Token(type, lexeme, null, peek().line, -1);
	}

	// Product a synthetic identifier so that we can keep parsing
	private Token syntheticIdent(String name) {
		return new Token(TokenType.IDENTIFIER, name, null, peek().line, -1);
	}
}
