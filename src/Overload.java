import java.util.*;

// Store overloads of functions
class OverloadSet {
	private final List<RainFunction> fns = new ArrayList<>();

	void add(RainFunction fn) { fns.add(fn); }
	RainFunction get(int i) { return fns.get(i); }
	int size() { return fns.size(); }

	@Override
	public String toString() { return "<overloads " + fns.size() + ">"; }
}
