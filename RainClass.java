import java.util.*;

class RainClass implements Callable {
	final String name;
	private final Environment closure;
	final Map<String, RainFunction> methods;
	final List<String> fieldNames;
	final List<Stmt> fieldInits;
	// Can be null
	final Stmt.Constructor ctor;

	RainClass(String name, Environment closure, Map<String, RainFunction> methods, List<String> fieldNames, List<Stmt> fieldInits, Stmt.Constructor ctor) {
		this.name = name;
		this.closure = closure;
		this.methods = methods;
		this.fieldNames = fieldNames;
		this.fieldInits = fieldInits;
		this.ctor = ctor;
	}

	@Override
	public int arity() {
		return (ctor == null) ? 0 : ctor.params.size();
	}

	@Override
	public Object call(Interpreter interpreter, Token paren, List<Object> args) {
		// Create instance and a base env with 'this'
		RainInstance instance = new RainInstance(this);

		// Pre-seed all declared fields to null (so cross-field refs in initialisers work)
		for (String f : fieldNames) {
			instance.defineField(f, null);
		}

		Environment base = new Environment(closure);
		base.define("this", instance);

		// Run field initialisers
		if (!fieldInits.isEmpty()) {
			interpreter.executeBlock(fieldInits, base);
		}

		// Run constructor (if any)
		if (ctor != null) {
			if (args.size() != ctor.params.size()) {
				throw new RainRuntimeError(paren, "Expected " + ctor.params.size() + " arguments but got " + args.size() + ".");
			}
			Environment cenv = new Environment(base);
			for (int i = 0; i < ctor.params.size(); i++) {
				Stmt.Param p = ctor.params.get(i);
				cenv.define(p.name.lexeme, args.get(i));
			}
			try {
				interpreter.executeBlock(ctor.body, cenv);
			} catch (RainReturn ignored) {
				// Constructors don't care about returns
			}
		} else if (!args.isEmpty()) {
			throw new RainRuntimeError(paren, "Expected 0 arguments but got " + args.size() + ".");
		}

		return instance;
	}

	@Override
	public String toString() { return "<class " + name + ">"; }
}