# Base Types
## Raw numbers
Every number is a 64-bit double. Numbers are defined using the `Val` type. They are dimensionless and because of this they can be freely mixed with other types without causing parsing or runtime errors.
```java
// Define a number with the value of 1
Val a = 1;
// Define a number with decimals
Val pi = 3.14159265;
// Values can be defined with percentages, internally this converts to decimal
Val half = 50%; // Internally represented as 0.5
// Most fundamental operations are supported: + - * /
```
## Body
A body is the fundamental node unit for RainLang. A body is defined as a surface area and an initial water volume. Bodies do not have capacity caps
```java
// Define a body with an area of 10m2 and 30L of initial water
Body a = Body(10m2, 30L);

println(a.volume);   // 30L
println(a.area);     // 10m2
println(a.sources);  // []
println(a.sinks);    // []
println(a.inflows);  // []
println(a.outflows); // []
```
## Volume
Volume is a representation of water in Litres with the literal `L`. When talking about bodies of water in real systems, the usage of just the literal `L` may cause issues with needing excessively large numbers. To resolve this, SI prefixes can be added to the literal to improve readability.
```java
// Define 10L of volume
Volume a = 10L;
// Define 1 million L
Volume b = 1000000L;
// Also define 1 million L but much more readable
Volume b2 = 1ML;
assert (b == b2); // True
// We support anywhere from L, kL, ML, GL, TL;
Volume[] volumes = [ 1L, 1kL, 1ML, 1GL, 1TL ];
// No support for PetaLitres or higher yet
Volume pl = 1PL; // Parsing error, unknown literal "PL"
```
## Area
Area is a representation of surface area in the base unit Metres<sup>2</sup> with the literal `m2`. Similar to bodies of water in real systems, the usage of just the literal `m2` may cause issues with needing excessively large numbers. Likewise, SI prefixes can resolve this. However only `km2` is supported for this
```java
// Define an area of 1m2
Area a = 1m2;
// Define 1,000,000 square metres of area
Area b = 1000000m2;
// A much more readable version
Area b2 = 1km2;
assert(b == b2); // True
// We only support m2 and km2
Area[] areas = [ 1m2, 1km2 ];
// No support for any other area yet
Volume cm = 1cm2; // Parsing error, unknown literal "cm2"
Volume mega = 1Mm2; // Parsing error, unknown literal "Mm2"
```
## Rain
Rain is a representation of the amount of rainfall in mm in a given area as measured by a standard rain gauge. It is denoted with the literal `mm`. There are no supported SI prefixes for this.
```java
// Define 1mm of rain
Rain a = 1mm;
// Define 100mm of rain
Rain b = 100mm;
```
## Arrays
Arrays allow for storing multiple values in 1 variable. Arrays do not need a fixed upfront size, they are dynamic and can be pushed, popped or modified at pleasure (mutable) while being transparent to the runtime system. Arrays are restricted to 1 dimensional (No arrays in arrays) and must be the same type throughout. Arrays also have properties that can be accessed
```java
// Define an array of numbers with some elements
Val[] arr = [ 1, 2, 3, 4 ];
// Define an array of percentages (Useful for kernels)
Val[] kernel = [ 40%, 30%, 20%, 10% ];
// Define an array of volumes
Volume[] volumes = [ 10L, 200L, 40kL, 500ML ];
// Arrays have a 1 property
// Get the size of the array, this is a Val type
print(arr.length); // Prints 4
// You can access array elements via indexing
println(arr[0]); // Prints 1
println(arr[arr.length - 1]); // Prints the last element, 4
// Arrays can be concatenated (As long as they are the same type)
Val[] a = [ 1, 2, 3 ];
Val[] b = [ 4, 5, 6 ];
Val[] c = a + b; // Becomes [ 1, 2, 3 ,4, 5, 6 ]
```
## Strings
Strings are mainly useful for making prints nicer. Strings are immutable, unsplittable (Cannot be split), unindexable (You cannot index into a string). However they are comparable (literal comparison). Strings only support standard ASCII characters and escape sequences are not supported
```java
// Define a string
String hello = "Hello world!";
// You can also use normal apostrophes
String hello2 = 'Hello world!';
// Strings can be compared
assert(hello == hello2); // True
// Case senstive comparisons
assert(hello != "hello world!"); // True
// Strings can be concatenated
String combined = hello + " Welcome to RainLang!"; // Becomes "Hello world! Welcome to RainLang!"
// Concatenated strings must be replaced: Immutabilty
hello += " Welcome to RainLang!"; // Parsing error: Unexpected operator "+="
// This works
hello = hello + " Welcome to RainLang!";
```
# Object property accessing
Some types have properties. These properties can be accessed using the `of` operator like so. The below code snippet demonstrates all built in objects and their properties
```java
// Body
Body a = 10m2, 30L;
println(a.volume); // Prints 30L, this is of type Volume
println(a.area); // prints 10m2, this is of type Area
// Arrays
Val[] arr = [ 1, 2, 3, 4 ];
println(arr.length); // Prints 4, this is of type Val
```
# Relationships
Adding, subtracting, multiplying and dividing numbers in different ways can lead to numbers transforming in units. The below tables represent allowed operations / transformed unit. Horizontally represents the first operand and vertical represents the 2nd operand

| + Add   | Val     | Body | Volume  | Area    | Rain    | Arrays        | Strings |
| ------- | ------- | ---- | ------- | ------- | ------- | ------------- | ------- |
| Val     | ✅       | ❌    | ❌       | ❌       | ❌       | ❌             | ✅ str() |
| Body    | ❌       | ❌    | ❌       | ❌       | ❌       | ❌             | ❌       |
| Volume  | ❌       | ❌    | ✅       | ❌       | ❌       | ❌             | ✅ str() |
| Area    | ❌       | ❌    | ❌       | ✅       | ❌       | ❌             | ❌       |
| Rain    | ❌       | ❌    | ❌       | ❌       | ✅       | ❌             | ✅ str() |
| Arrays  | ❌       | ❌    | ❌       | ❌       | ❌       | ✅ (same type) | ✅ str() |
| Strings | ✅ str() | ❌    | ✅ str() | ✅ str() | ✅ str() | ✅ str()       | ✅       |

| - Subtract | Val | Body | Volume | Area | Rain | Arrays | Strings |
| ---------- | --- | ---- | -------- | ---- | ---- | ------ | ------- |
| Val        | ✅   | ❌    | ❌        | ❌    | ❌    | ❌      | ❌       |
| Body       | ❌   | ❌    | ❌        | ❌    | ❌    | ❌      | ❌       |
| Volume   | ❌   | ❌    | ✅        | ❌    | ❌    | ❌      | ❌       |
| Area       | ❌   | ❌    | ❌        | ✅    | ❌    | ❌      | ❌       |
| Rain       | ❌   | ❌    | ❌        | ❌    | ✅    | ❌      | ❌       |
| Arrays     | ❌   | ❌    | ❌        | ❌    | ❌    | ❌      | ❌       |
| Strings    | ❌   | ❌    | ❌        | ❌    | ❌    | ❌      | ❌       |

| * Multiply | Val | Body | Volume | Area       | Rain       | Arrays | Strings |
| ---------- | --- | ---- | -------- | ---------- | ---------- | ------ | ------- |
| Val        | ✅   | ❌    | ✅        | ✅          | ✅          | ❌      | ❌       |
| Body       | ❌   | ❌    | ❌        | ❌          | ❌          | ❌      | ❌       |
| Volume   | ✅   | ❌    | ❌        | ❌          | ❌          | ❌      | ❌       |
| Area       | ✅   | ❌    | ❌        | ❌          | ✅ Volume | ❌      | ❌       |
| Rain       | ✅   | ❌    | ❌        | ✅ Volume | ❌          | ❌      | ❌       |
| Arrays     | ❌   | ❌    | ❌        | ❌          | ❌          | ❌      | ❌       |
| Strings    | ❌   | ❌    | ❌        | ❌          | ❌          | ❌      | ❌       |

| / Divide | Val | Body | Volume | Area | Rain | Arrays | Strings |
| -------- | --- | ---- | ------ | ---- | ---- | ------ | ------- |
| Val      | ✅   | ❌    | ❌      | ❌    | ❌    | ❌      | ❌       |
| Body     | ❌   | ❌    | ❌      | ❌    | ❌    | ❌      | ❌       |
| Volume   | ✅   | ❌    | ❌      | ❌    | ❌    | ❌      | ❌       |
| Area     | ✅   | ❌    | ✅ Rain | ❌    | ❌    | ❌      | ❌       |
| Rain     | ✅   | ❌    | ✅ Area | ❌    | ❌    | ❌      | ❌       |
| Arrays   | ❌   | ❌    | ❌      | ❌    | ❌    | ❌      | ❌       |
| Strings  | ❌   | ❌    | ❌      | ❌    | ❌    | ❌      | ❌       |
# Booleans
Booleans are a special type as they are only compatible with logical operators (`<, <=, ==, !=, >, >=`) and binary operators (`&&, ||, !`) on themselves. I'm sure you know how bools work so I CBF to write out how they work