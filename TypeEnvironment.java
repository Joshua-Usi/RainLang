import java.util.*;

class TypeEnvironment {
	private final Deque<Map<String, Type>> scopes = new ArrayDeque<>();
	TypeEnvironment() { scopes.push(new HashMap<>()); }
	void clear() { scopes.clear(); scopes.push(new HashMap<>()); }
	void push() { scopes.push(new HashMap<>()); }
	void pop() { scopes.pop(); }
	void define(String name, Type type) { scopes.peek().put(name, type); }
	Type lookup(String name) {
		for (Map<String, Type> s : scopes)
			if (s.containsKey(name)) return s.get(name);
		return null;
	}
}
