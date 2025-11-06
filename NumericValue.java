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
		// Format to max 3 decimal places
		String num = String.format("%.3f", v);
		// Strip trailing zeros
		num = num.replaceAll("0+$", "");
		// Strip trailing decimal point
		num = num.replaceAll("\\.$", "");

		switch (t.kind) {
			case VOLUME: return num + "L";
			case RAIN:   return num + "mm";
			case AREA:   return num + "m2";
			case VAL:    return num;
			default:     return num;
		}
	}
}