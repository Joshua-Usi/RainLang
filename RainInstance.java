import java.util.*;

class RainInstance {
	private final RainClass klass;
	private final Map<String, Object> fields = new HashMap<>();

	RainInstance(RainClass klass) { this.klass = klass; }

	// Preseed fields
	void defineField(String name, Object value) {
		fields.put(name, value);
	}

	Object get(Token name) {
		if (fields.containsKey(name.lexeme)) return fields.get(name.lexeme);
		RainFunction method = klass.methods.get(name.lexeme);
		if (method != null) return method.bind(this);
		throw new RainRuntimeError(name, "Undefined property '" + name.lexeme + "'.");
	}

	void set(Token name, Object value) {
		if (klass.methods.containsKey(name.lexeme)) {
			throw new RainRuntimeError(name, "Cannot assign to method '" + name.lexeme + "'.");
		}
		fields.put(name.lexeme, value);
	}

	@Override
	public String toString() { return "<" + klass.name + " instance>"; }
}