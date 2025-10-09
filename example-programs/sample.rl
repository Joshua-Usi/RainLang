// Test basic water body creation and connections
Body dam1 = Body(500m2, 100000l);
Body river = Body(200m2, 50000l);

connect(dam1, river, 1000l);
output(river);

// Add initial water
source(dam1, 50000l);

print("Initial state:");
print("Dam1 level: " + dam1.volume);
print("River level: " + river.volume);

// Simulate one day
simulate();

print("After 1 day:");
print("Dam1 level: " + dam1.volume);
print("River level: " + river.volume);