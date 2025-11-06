import java.util.*;

class TypeEnvironment {
	private final Deque<Map<String, Type>> scopes = new ArrayDeque<>();
	private final Deque<Map<String, List<Type>>> fnScopes = new ArrayDeque<>();

	TypeEnvironment() { scopes.push(new HashMap<>()); fnScopes.push(new HashMap<>()); }
	void clear() { scopes.clear(); scopes.push(new HashMap<>()); fnScopes.clear(); fnScopes.push(new HashMap<>()); }
	void push() { scopes.push(new HashMap<>()); fnScopes.push(new HashMap<>()); }
	void pop() { scopes.pop(); fnScopes.pop(); }
	void define(String name, Type type) { scopes.peek().put(name, type); }

	void addFunctionOverload(String name, Type fnType) {
		Map<String, List<Type>> top = fnScopes.peek();
		List<Type> list = top.get(name);
		if (list == null) {
			list = new ArrayList<>();
			top.put(name, list);
		}
		list.add(fnType);
	}

	List<Type> lookupFunctionOverloads(String name) {
		for (Map<String, List<Type>> s : fnScopes) {
			if (s.containsKey(name)) return s.get(name);
		}
		return null;
	}

	Type lookup(String name) {
		for (Map<String, Type> s : scopes)
			if (s.containsKey(name)) return s.get(name);
		return null;
	}
}
