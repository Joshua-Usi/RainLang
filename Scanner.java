import java.util.*;

class Tuple<X, Y> { 
	public final X x; 
	public final Y y; 
	public Tuple(X x, Y y) { 
		this.x = x; 
		this.y = y; 
	}
} 

public class Scanner {
	private final String source;
	private final List<Token> tokens = new ArrayList<Token>();

	private int start = 0;
	private int current = 0;
	private int line = 1;

	private static final Map<String, TokenType> keywords = new HashMap<>();
	static {
		// Boolean keywords
		keywords.put("true", TokenType.TRUE);
		keywords.put("false", TokenType.FALSE);
		// Control flow keywords
		keywords.put("if", TokenType.IF);
		keywords.put("else", TokenType.ELSE);
		keywords.put("while", TokenType.WHILE);
		keywords.put("for", TokenType.FOR);
		keywords.put("break", TokenType.BREAK);
		keywords.put("continue", TokenType.CONTINUE);
		// High level concepts
		keywords.put("return", TokenType.RETURN);
		keywords.put("class", TokenType.CLASS);
		keywords.put("super", TokenType.SUPER);
		keywords.put("this", TokenType.THIS);
		// None
		keywords.put("None", TokenType.NONE);
	}

	public Scanner(String source) {
		this.source = source;
	}
	public List<Token> tokenise() {
		while (!isAtEnd()) {
			// Beginning of next lexeme
			this.start = this.current;
			scanToken();
		}
		
		// Denote end of input
		// TODO support columns
		tokens.add(new Token(TokenType.EOF, "", null, line, -1));
		return tokens;
	}
	// Figure out what type of token we are looking at
	private void scanToken() {
		char c = advance();
		switch (c) {
			// Contol characters
			case '(': addToken(TokenType.LEFT_PAREN); break;
			case ')': addToken(TokenType.RIGHT_PAREN); break;
			case '{': addToken(TokenType.LEFT_BRACE); break;
			case '}': addToken(TokenType.RIGHT_BRACE); break;
			case '[': addToken(TokenType.LEFT_BRACKET); break;
			case ']': addToken(TokenType.RIGHT_BRACKET); break;	
			case ',': addToken(TokenType.COMMA); break;
			case '.': addToken(TokenType.DOT); break;
			case ';': addToken(TokenType.SEMICOLON); break;
			// Numerical operators
			case '-': addToken(TokenType.MINUS); break;
			case '+': addToken(TokenType.PLUS); break;
			case '*': addToken(TokenType.STAR); break;
			case '/': {
				// Check for comments, we don't care about them so we don't give them a token
				if (match('/')) {
					while (peek() != '\n' && !isAtEnd()) advance();
				} else {
					addToken(TokenType.SLASH);
				}
				break;
			}
			// Logical operators
			case '&': if (match('&')) { addToken(TokenType.AND_AND); break; }
			case '|': if (match('|')) { addToken(TokenType.OR_OR); break; }
			// Comparison operators
			case '!': addToken(match('=') ? TokenType.BANG_EQUAL    : TokenType.BANG); break;
			case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL   : TokenType.EQUAL); break;
			case '<': addToken(match('=') ? TokenType.LESS_EQUAL    : TokenType.LESS); break;
			case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
			// Ignore whitespace
			case ' ':
			case '\r':
			case '\t':
				break;
			case '\n': line++; break;
			// Handling strings
			case '"': handleString(); break;
			// Unknown characters
			default:
				if (isDigit(c)) {
					handleNumber();
				} else if (isAlpha(c) || c == '%') {
					handleIdentifier();
				} else {	
					RainLang.error(line, "Unexpected character \"" + c + "\"");
				}
				break;
		}
	}
	// Denotes if we are at the end of our input
	private boolean isAtEnd() {
		return current >= source.length();
	}
	// Advance to the next character
	private char advance() {
		return source.charAt(current++);
	}
	// Like advance, but doesn't consume the character
	private char peek(int lookahead) {
		if (current + lookahead >= source.length()) return '\0';
		return source.charAt(current + lookahead);
	}
	private char peek() {
		return peek(0);
	}
	private boolean match(char expected) {
		if (isAtEnd()) return false;
		if (source.charAt(current) != expected) return false;

		current++;
		return true;
	}
	private void addToken(TokenType type) {
		addToken(type, null);
	}
	private void addToken(TokenType type, Object literal) {
		String text = source.substring(start, current);
		// TODO support columns
		tokens.add(new Token(type, text, literal, this.line, -1));
	}
	private static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}
	private static boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
	}
	private static boolean isAlphaNumeric(char c) {
		// Fuck it, this lets us define variables with percent signs in them, but yolo
		return isAlpha(c) || isDigit(c) || c == '%';
	}
	private void handleString() {
		while (peek() != '\"' && !isAtEnd()) {
			if (peek() == '\n') line++;
			advance();
		}
		if (isAtEnd()) {
			RainLang.error(line, "Unterminated string.");
			return;
		}
		// Closing "
		advance();
		// Trim the quotes
		String value = source.substring(start + 1, current - 1);
		addToken(TokenType.STRING, value);
	}
	private void handleNumber() {
		// Consume as many integer parts as we can
		while (isDigit(peek())) advance();
		// Look for fractional part
		if (peek() == '.' && isDigit(peek(1))) {
			// Consume the .
			advance();
			// Consume fractional parts
			while (isDigit(peek())) advance();
		}
		addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
	}
	private void handleIdentifier() {
		// Maximal munch, consume as much as we can
		while (isAlphaNumeric(peek())) advance();
		String text = source.substring(start, current);
		// Check if it's a keyword
		TokenType type = keywords.get(text);
		// If not, it's an identifer
		if (type == null) type = TokenType.IDENTIFIER;
		addToken(type);
	}
}