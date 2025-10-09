public enum TokenType {
	// Control characters
	LEFT_PAREN, RIGHT_PAREN,
	LEFT_BRACE, RIGHT_BRACE,
	LEFT_BRACKET, RIGHT_BRACKET,
	COMMA, DOT, SEMICOLON,

	// Numerical operators
	MINUS, PLUS, SLASH, STAR,

	// Control flow keywords
	IF, ELSE, WHILE, FOR,

	// Boolean keywords
	TRUE, FALSE,

	// Boolean operators
	BANG, BANG_EQUAL,
	EQUAL, EQUAL_EQUAL,
	GREATER, GREATER_EQUAL,
	LESS, LESS_EQUAL,
	AND_AND, OR_OR,

	// High-level concepts
	RETURN,
	CLASS, SUPER, THIS,

	// Literals
	NUMBER, STRING, IDENTIFIER, NONE,

	EOF
}