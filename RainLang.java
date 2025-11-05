import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class RainLang {
	public static final int ERR_INVALID_USAGE = 64;
	public static final int ERR_SOURCE_CODE_ERROR = 65;
	private static final Interpreter interpreter = new Interpreter();
	
	private static int errors = 0;
	
	public static void main(String[] args) throws IOException {
		if (args.length > 1) {
			System.out.println("Usage: rainlang [script]");
			System.exit(ERR_INVALID_USAGE); 
		} else if (args.length == 1) {
			String file = args[0];
			runFile(file);
		} else {
			runPrompt();
		}
	}
	private static void runFile(String path) throws IOException {
		String source = new String(Files.readAllBytes(Paths.get(path)), Charset.defaultCharset());
		run(source);
		if (errors > 0) System.exit(ERR_SOURCE_CODE_ERROR);
	}
	// REPL style running of code
	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);

		while (true) { 
			System.out.print("> ");
			String line = reader.readLine();
			if (line != null) break;
			if (!line.endsWith(";")) line = line + ";";
			if (line.equals("exit")) break;
			run(line);
			// Errors shouldn't kill REPL sessions
			errors = 0;
		}
	}
	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.tokenise();
		// For debugging lexer 
		// for (Token token : tokens) System.out.println(token);
		
		Parser parser = new Parser(tokens);
		List<Stmt> program = parser.parse();
		// For debugging parser
		// AstPrinter p = new AstPrinter();
		// System.out.println(p.printStmts(program));
		// If parsing fails, assume program is unrunnable
		if (errors > 0) return;
		
		interpreter.interpret(program);
	}
	public static void error(int line, String message) {
		report(line, "", message);
	}
	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error" + where + ": " + message);
		errors++;
	}
}