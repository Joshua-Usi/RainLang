import java.util.*;

public class Type {
	enum Kind {
		VAL, VOLUME, AREA, RAIN,
		BOOL, STRING, NONE,
		ARRAY, CLASS,
		FUNCTION,
		UNKNOWN
	}

	final Kind kind;
	// For arrays
	final Type element;
	// For classes
	final String name;
	// For functions
	final Type returnType;
	final List<Type> paramTypes;

	private Type(Kind kind, Type element, String name, Type returnType, List<Type> paramTypes) {
		this.kind = kind;
		this.element = element;
		this.name = name;
		this.returnType = returnType;
		this.paramTypes = paramTypes;
	}

	// Factory methods (you likely already have the first ones)
	static Type val()     { return new Type(Kind.VAL, null, null, null, null); }
	static Type volume()  { return new Type(Kind.VOLUME, null, null, null, null); }
	static Type area()    { return new Type(Kind.AREA, null, null, null, null); }
	static Type rain()    { return new Type(Kind.RAIN, null, null, null, null); }
	static Type bool()    { return new Type(Kind.BOOL, null, null, null, null); }
	static Type string()  { return new Type(Kind.STRING, null, null, null, null); }
	static Type none()    { return new Type(Kind.NONE, null, null, null, null); }
	static Type unknown() { return new Type(Kind.UNKNOWN, null, null, null, null); }

	static Type arrayOf(Type element) {
		return new Type(Kind.ARRAY, element, null, null, null);
	}

	static Type classType(String name) {
		return new Type(Kind.CLASS, null, name, null, null);
	}

	static Type function(Type returnType, List<Type> paramTypes) {
		return new Type(Kind.FUNCTION, null, null, returnType, paramTypes);
	}

	boolean isNumericDomain() {
		return kind == Kind.VAL || kind == Kind.VOLUME || kind == Kind.RAIN || kind == Kind.AREA;
	}

	@Override
	public String toString() {
		switch (kind) {
			case ARRAY: return element + "[]";
			case CLASS: return name;
			case FUNCTION:
				return "(" + String.join(", ", paramTypes.stream().map(Type::toString).toList()) + ") -> " + returnType;
			default:
				String raw = kind.name().toLowerCase();
				return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
		}
	}

	@Override
public boolean equals(Object o) {
	if (this == o) return true;
	if (!(o instanceof Type)) return false;
	Type other = (Type) o;

	if (this.kind != other.kind) return false;

	switch (kind) {
		case ARRAY:
			return this.element.equals(other.element);

		case CLASS:
			return Objects.equals(this.name, other.name);

		case FUNCTION:
			// return types must match
			if (!this.returnType.equals(other.returnType)) return false;
			// parameter lists must match length + element types
			return Objects.equals(this.paramTypes, other.paramTypes);

		default:
			// Primitive and domain types compare by kind only
			return true;
	}
}
}