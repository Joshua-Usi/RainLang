class RainRuntimeError extends RuntimeException {
	final Token token;

	RainRuntimeError(Token token, String message) {
		super(message);
		this.token = token;
	}
}
