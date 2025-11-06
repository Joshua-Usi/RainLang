import java.util.*;

final class CallResolution {
	static final class Resolved {
		final String name;
		final int slot;
		Resolved(String name, int slot) { this.name = name; this.slot = slot; }
	}

	private static final Map<Expr.Call, Resolved> map = Collections.synchronizedMap(new WeakHashMap<>());

	// Determine which calls things map to
	static void bind(Expr.Call call, String name, int slot) { map.put(call, new Resolved(name, slot)); }
	static Resolved get(Expr.Call call) { return map.get(call); }
}