Body a = Body(2km2, 0L);
Body b = Body(1km2, 0L);
// Connect a -> b
connect(a, b);
// Rain event over a
rain(a, 20mm);
// Simulate 5 days
simulate(5);
// Conditional check
if (b.volume > 100kL) {
    println("High flow at b");
} else {
    println("Normal flow at b");
}