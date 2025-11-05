import java.util.*;

class Builtins {

	static void registerRuntime(Environment env) {
		// Utility
		env.define("print", new Callable() {
			@Override public int arity() { return 1; }
			@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) { System.out.print(interpreter.display(args.get(0))); return null; }
			@Override public String toString() { return "<native print fn>"; }
		});
		env.define("println", new Callable() {
			@Override public int arity() { return 1; }
			@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) { System.out.println(interpreter.display(args.get(0))); return null; }
			@Override public String toString() { return "<native println fn>"; }
		});

		env.define("assert", new Callable() {
			@Override public int arity() { return 1; }
			@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) {
				if (!interpreter.truthy(args.get(0))) {
					throw new RainRuntimeError(paren, "Assertion failed: " + interpreter.display(args.get(0)));
				}
				return null;
			}
			@Override public String toString() { return "<native assert fn>"; }
		});

		// Hydrology
		env.define("connect", new Callable() {
			@Override public int arity() { return 2; }
			@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) { return null; }
			@Override public String toString() { return "<native connect fn>"; }
		});
		env.define("disconnect", new Callable() {
			@Override public int arity() { return 2; }
			@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) { return null; }
			@Override public String toString() { return "<native disconnect fn>"; }
		});

		env.define("source", new Callable() {
			@Override public int arity() { return 2; }
			@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) { return null; }
			@Override public String toString() { return "<native source fn>"; }
		});
		env.define("sink", new Callable() {
			@Override public int arity() { return 2; }
			@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) { return null; }
			@Override public String toString() { return "<native sink fn>"; }
		});

		env.define("rain", new Callable() {
			@Override public int arity() { return 3; }
			@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) { return null; }
			@Override public String toString() { return "<native rain fn>"; }
		});

		env.define("simulate", new Callable() {
			@Override public int arity() { return 1; }
			@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) { return null; }
			@Override public String toString() { return "<native simulate fn>"; }
		});
	}

	static void registerTypes(TypeEnvironment tenv) {
		tenv.define("print", Type.function(Type.none(), List.of(Type.string())));
		tenv.define("println", Type.function(Type.none(), List.of(Type.string())));
		tenv.define("assert", Type.function(Type.none(), List.of(Type.bool())));

		tenv.define("connect", Type.function(Type.none(), List.of(Type.classType("Body"), Type.classType("Body"))));
		tenv.define("disconnect", Type.function(Type.none(), List.of(Type.classType("Body"), Type.classType("Body"))));

		tenv.define("source", Type.function(Type.none(), List.of(Type.classType("Body"), Type.volume())));
		tenv.define("sink", Type.function(Type.none(), List.of(Type.classType("Body"), Type.volume())));
	}
}
