Val fib(Val n) {
	if (n <= 1) {
		return n;
	}
	// Naive fibonacci, very computationally inefficient, but works for proving recursion works
	return fib(n - 1) + fib(n - 2);
}

print(fib(0));  // 0
print(fib(1));  // 1
print(fib(5));  // 5
print(fib(10)); // 55
