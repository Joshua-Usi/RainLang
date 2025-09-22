```java
// A body is constructed with a surface area and a initial capacity
Body a = 10m2, 30L; // Defines a body a with surface area 10m2 and with 30l of initial water
Body b = 10m2, 10L; // Defines a body b with surface area 10m2 and with 10l of initial water

// Every number is a double, double numbers don't have a unit so they aren't very type-safe but they are flexible
Number number = 492.03203203;
// Percentage literals, under the hood convert to 0.5
Number percent = 50%;
// Arrays are allowed
Number[] arr = [ 1.23, 4, 423, 2, 32 ];
// Capacity literal is l
Capacity capacity = 10L;
// Area literal is m2
Area a = 10m2;
// Rain literal is mm
Rain r = 10mm;

// We assume the 4 basic operators. That's it
// +, -, *, /

// Unit conversions
// Capacity / Area = Rain;
// Rain * Area = Capacity;
// Capacity / Rain = Area;

// Creates a connection to exclusively flow from a to b with a maximum of 5l per day
// Check for cycles, program throws if there is one. We allow for bifurcations and joins, but no cycle
connect a, b, 5l;

// Splits
// connect a, b;
// connect a, c;

// Joins
// connect b, a;
// connect c, a;

// Removes a connection
// disconnect a, b;

// Designate b to be the output river
output b;

// Adds 5l of water without question to body A
source a, 5l;
// Removes 5l of water without question from Body b
sink b, 5l;

// 10mm of rain falls on a, with a 10m^2 surface area that is 100l of water, so a has 200l of water (after the kernel finishes), in the first day, a will have 140l, the 2nd (assuming no outflow): 170l, etc, etc
rain 10mm, a, [ 40%, 30%, 20%, 10% ];

// Simulates a day
simulate;

// Simulates multiple days
// simulate 2;

// In this day
// a gains 5l from source
// b loses 5l from sink

// For connections half of the difference between two nodes, with respect to maximum capacity is moved
// 130l - 10 = 120l / 2 = 60l should flow, but limited by flow rate
// Hence a loses 5l flowing to b
// b gains 5l from a
// Flow rate is at limit by connection limit

// Hence overall
// a has 130l of water (130 + 5 - 5)
// b has 10l of water (10 - 5 + 5)

// Flow principles
// 1. All sources are applied
// 2. Rainfall kernels are applied
// 3. Then we find the "highest" river and apply flow downward
// 4. Sinks are then applied

// to access objects, we use <field> of <object>
print level of a;
// We can also get the area
print area of a;

// Python like if statements
// We allow for this binary + 1 unary operator
// Parenthesis can be used for ambiguity resolution. but otherwise, normal precendence applies: And before or
// <, <=, >, >=, ==, and, or, not
if level of a < 30: {
    println "IM RUNNING LOW"
}

// For loops also available
while True: {
  println "IM LOOPING"
}

// Function signature <name>: {<arguments}:
fun sum: arg1, arg2: {
	return arg1 + arg2;
}

Number a = sum 1, 2;
```