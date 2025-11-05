// Test basic water body creation and connections
Body dam1 = Body(500m2, 100000L);
Body river = Body(200m2, 50000L);

connect(dam1, river, 1000L);
output(river);

// Add initial water
source(dam1, 50000L);

print("Initial state:");
print("Dam1 level: " + dam1.volume);
print("River level: " + river.volume);

// Simulate one day
simulate();

print("After 1 day:");
print("Dam1 level: " + dam1.volume);
print("River level: " + river.volume);