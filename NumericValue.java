// Lets us print nice suffixes for types
class NumericValue {
	final Type type;
	final double value;

	NumericValue(Type type, double value) {
		this.type = type;
		this.value = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof NumericValue other)) return false;
		return this.type.equals(other.type)
			&& this.value == other.value;
	}

	@Override
	public String toString() {
		return typeToSuffix(type, value);
	}

	static String typeToSuffix(Type t, double v) {
		switch (t.kind) {
			case VOLUME: return v + "L";
			case RAIN:   return v + "mm";
			case AREA:   return v + "m2";
			case VAL:    return String.valueOf(v);
			default:     return String.valueOf(v);
		}
	}
}