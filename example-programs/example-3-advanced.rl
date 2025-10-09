Body dam = Body(5km2, 50kL);
// Define a river to drain into
Body river = Body(2km2, 0L);
connect(dam, river);
// Declaring a function
None rain_and_report(Body target, Rain r, Val days) {
    rain(target, r);
    for (Val i = 0; i < days; i++) {
        println("Day " + str(i) + ": " + str(target.volume));
        simulate();
    }
}
// Using a custom function
rain_and_report(dam, 15mm, 5);