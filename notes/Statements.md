# if
General syntax:
```java
if (expression) {
	// Do something
}

if (expression) {
	// Do something
} else {
  // Do something else
}

if (expression) {
	// Do something
} else if (expression2) {
	// Do another thing
} else {
	// Do something else
}
```
# while
General syntax:
```java
while (expression) {
	// Do something
}
```
# for
General syntax:
```
for (Val i = 0; i < 100; i++) {
		// Do something
}
```
# Functions
```
// Static typed function
Val function_name(Val arg1, Body arg2) {
	// function body...
	// Optional return, returns can be anywhere in the function flow, doesn't need to be at the end
	return 1;
}
```
# Classes
```
class ClassName {
	// Assume everything is public
	Val a;
	Body b;

	// Constructor
	ClassName(Val a, Body b) {
		this.a = a;
		this.b = b;
	}
	// Methods
	Val sum(Val a, Val b) {
		return a + b;
	}
}
```
