import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class RainLang {
	public static final int ERR_INVALID_USAGE = 64;
	public static final int ERR_SOURCE_CODE_ERROR = 65;
	private static final Interpreter interpreter = new Interpreter();
	private static final SemanticAnalyser semanal = new SemanticAnalyser();
	
	private static int errors = 0;

	private static boolean stdlibLoaded = false;
	private static final String STDLIB_RESOURCE = "standard_lib.txt";

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
		ensureStdlibLoaded();
		if (errors > 0) System.exit(ERR_SOURCE_CODE_ERROR);

		String source = new String(Files.readAllBytes(Paths.get(path)), Charset.defaultCharset());
		run(source);

		run("hydrology_report_implicit();");

		if (errors > 0) System.exit(ERR_SOURCE_CODE_ERROR);
	}

	// REPL style running of code
	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);

		interpreter.setReplMode(true);

		ensureStdlibLoaded();
		if (errors > 0) return;

		while (true) { 
			System.out.print("> ");
			String line = reader.readLine();
			if (line == null) continue;
			if (line.length() == 0) continue;
			if (line.contains("exit")) break;
			char last = line.charAt(line.length() - 1);
			if (last != ';' && last != '}') {
				line = line + ";";
			}
			run(line);
			// Errors shouldn't kill REPL sessions
			errors = 0;
		}
	}

	private static void run(String source) {
		ensureStdlibLoaded();
		if (errors > 0) {
			System.out.println(errors + " Errors.");
			return;
		}

		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.tokenise();

		Parser parser = new Parser(tokens);
		List<Stmt> program = parser.parse();

		semanal.analyse(program);

		if (errors > 0) {
			System.out.println(errors + " Errors.");
			return;
		}

		interpreter.interpret(program);
	}

	public static void error(int line, String message) {
		report(line, "", message);
	}

	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error" + where + ": " + message);
		errors++;
	}

	private static void ensureStdlibLoaded() {
		if (stdlibLoaded) return;

		InputStream in = RainLang.class.getResourceAsStream(STDLIB_RESOURCE);
		if (in == null) {
			System.err.println("Fatal: Could not load standard library at " + STDLIB_RESOURCE);
			errors++;
			return;
		}

		String source;
		try {
			source = new String(in.readAllBytes(), Charset.defaultCharset());
		} catch (IOException e) {
			System.err.println("Fatal: Failed reading standard library: " + e.getMessage());
			errors++;
			return;
		}

		compileAndRunUnit(source);
		if (errors == 0) stdlibLoaded = true;
	}

	private static void compileAndRunUnit(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.tokenise();

		Parser parser = new Parser(tokens);
		List<Stmt> program = parser.parse();

		semanal.analyse(program);
		if (errors > 0) return;

		interpreter.interpret(program);
	}
}