import java.util.*;

class Environment {
	// Parent scope
	final Environment enclosing;
	private final Map<String, Object> values = new HashMap<>();

	// Global scope
	Environment() {
		this.enclosing = null;
	}

	// Nested scope
	Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}

	void define(String name, Object value) {
		values.put(name, value);
	}

	boolean hasLocal(String name) {
		return values.containsKey(name);
	}

	Object getLocal(String name) {
		return values.get(name);
	}

	Object get(Token name) {
		if (values.containsKey(name.lexeme)) {
			return values.get(name.lexeme);
		}
		if (enclosing != null) return enclosing.get(name);
		throw new RainRuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	void assign(Token name, Object value) {
		if (values.containsKey(name.lexeme)) {
			values.put(name.lexeme, value);
			return;
		}
		if (enclosing != null) {
			enclosing.assign(name, value);
			return;
		}
		throw new RainRuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}
}
