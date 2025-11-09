Body[] river = Body(1km2, 0L);
// Apply rainfall
rain(river, 10mm);
// Run simulation for 1 day
simulate(1);
// Print final volume
println(river.volume);