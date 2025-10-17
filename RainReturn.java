class RainReturn extends RuntimeException {
	final Object value;
	RainReturn(Object value) {
		// What in the absolutely fuck?
		// Stops it from outputting stack traces
		super(null, null, false, false);
		this.value = value;
	}
}
