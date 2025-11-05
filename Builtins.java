import java.util.*;

class Builtins {

	static void registerRuntime(Environment env) {
		// Utility
		env.define("print", new Callable() {
			@Override public int arity() { return 1; }
			@Override public Object call(Interpreter interpreter, Token paren, List<Object> args) { System.out.println(interpreter.display(args.get(0))); return null; }
			@Override public String toString() { return "<native print fn>"; }
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
	}

	static void registerTypes(TypeEnvironment tenv) {
		tenv.define("print", Type.function(Type.none(), List.of(Type.string())));
		tenv.define("assert", Type.function(Type.none(), List.of(Type.bool())));
	}
}
