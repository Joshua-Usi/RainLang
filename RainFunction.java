import java.util.*;

class RainFunction implements Callable {
	private final Stmt.Function declaration;
	private final Environment closure;

	RainFunction(Stmt.Function declaration, Environment closure) {
		this.declaration = declaration;
		this.closure = closure;
	}

	// For classes
	RainFunction bind(RainInstance instance) {
		Environment env = new Environment(closure);
		env.define("this", instance);
		return new RainFunction(declaration, env);
	}

	@Override
	public int arity() {
		return declaration.params.size();
	}

	@Override
	public Object call(Interpreter interpreter, Token paren, List<Object> args) {
		Environment env = new Environment(closure);

		// Bind parameters
		for (int i = 0; i < declaration.params.size(); i++) {
			Stmt.Param param = declaration.params.get(i);
			env.define(param.name.lexeme, args.get(i));
		}

		try {
			interpreter.executeBlock(declaration.body, env);
		} catch (RainReturn returnValue) {
			return returnValue.value;
		}

		return null;
	}

	@Override
	public String toString() {
		return "<fn " + declaration.name.lexeme + ">";
	}
}