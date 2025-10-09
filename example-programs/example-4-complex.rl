// --- System setup (bodies, areas, initial volumes)
Body googong         = Body(3km2,  50kL);
Body jerrabombarra   = Body(2km2,  10kL);
Body upper_molongolo = Body(4km2,  20kL);
Body queanbeyan      = Body(1km2,   0L);
Body central_molongolo = Body(6km2,  0L);
Body dam1            = Body(0.4km2, 0L);
Body dam2            = Body(0.6km2, 0L);

// --- Graph wiring (directed)
connect(googong, dam1);
connect(dam1, queanbeyan);
connect(jerrabombarra, central_molongolo);
connect(upper_molongolo, central_molongolo);
connect(queanbeyan, central_molongolo);
connect(central_molongolo, dam2);

// --- Persistent sources/sinks (daily)
source(googong, 5kL);
sink(dam2, 2kL);

// --- Utility arrays & labels
Body[] rivers = [ googong, jerrabombarra, upper_molongolo, queanbeyan, central_molongolo, dam1, dam2 ];
String[] names = [ "googong", "jerrabombarra", "upper_molongolo", "queanbeyan", "central_molongolo", "dam1", "dam2" ];

// --- Kernels (fractions summing to ~1.0). Mutate to test Index/IndexSet.
Val[] wk = [ 0.40, 0.30, 0.20, 0.10 ];
wk[0] = 0.50;
wk[1] = 0.25; 

// --- Class exercising fields, constructor, methods, this, arithmetic with units
class Reservoir {
    Volume capacity;
    Body pool;

    Reservoir(Volume cap, Body b) {
        this.capacity = cap;
        this.pool = b;
    }

    Val percent_full() {
        // (pool.volume / capacity) -> Val, then * 100
        return (this.pool.volume / this.capacity) * 100;
    }

    None spill_if_over(Volume rule) {
        if (this.pool.volume > rule) {
            Volume extra = this.pool.volume - rule;
            // Model a one-off manual drawdown using sink() + one day simulate
            sink(this.pool, extra);
            simulate();
        }
    }
}

// Instantiate reservoirs around bodies
Reservoir R1 = Reservoir(200ML, dam1);
Reservoir R2 = Reservoir(500ML, dam2);

// --- Functions (params, returns, loops, calls, strings)
Volume max_volume(Body[] bs) {
    // Track the maximum Volume among bodies (simple linear scan)
    Volume best = 0L;
    Val i = 0;
    while (i < bs.length) {
        if (bs[i].volume > best) {
            best = bs[i].volume;
        }
        i = i + 1;
    }
    return best;
}

None print_report(Body[] bs, String[] labels) {
    Val i = 0;
    while (i < bs.length) {
        println(labels[i] + ": area=" + str(bs[i].area) + ", volume=" + str(bs[i].volume));
        i = i + 1;
    }
}

None apply_kernel_rain(Body target, Rain daily, Val[] kernel) {
    // Distribute one notional “event” using the kernel over consecutive days
    Val i = 0;
    while (i < kernel.length) {
        // kernel[i] is Val (fraction); Rain * Val -> Rain
        Rain portion = daily * kernel[i];
        rain(target, portion);
        simulate(); // one day advances sources/sinks + wavefront propagation
        i = i + 1;
    }
}

// --- Scenario A: pre-wet catchments, run a week
apply_kernel_rain(googong, 25mm, wk);
apply_kernel_rain(jerrabombarra, 12mm, wk);
apply_kernel_rain(upper_molongolo, 18mm, wk);

// Mid-scenario check & messaging (logical AND/OR with parentheses)
if ( (R1.percent_full() > 80) && (R2.percent_full() > 60) ) {
    println("Both dams healthy (R1>80%, R2>60%).");
} else if ( (R1.percent_full() < 30) || (R2.percent_full() < 30) ) {
    println("Warning: at least one dam under 30%.");
} else {
    println("Mixed conditions; proceeding.");
}

// --- Scenario B: targeted rainfall over central, then sustained simulation
for (Val d = 0; d < 7; d = d + 1) {
    rain(central_molongolo, 8mm);
    simulate();
}

// --- Scenario C: balance check loop until thresholds or day cap
Val day = 0;
while ( (day < 30) && (central_molongolo.volume < 200ML) ) {
    // Light daily shower on all headwaters
    rain(googong, 3mm);
    rain(jerrabombarra, 2mm);
    rain(upper_molongolo, 4mm);
    simulate();
    day = day + 1;
}

// Reservoir operational rule: keep dam2 below 450ML
R2.spill_if_over(450ML);

// Quick topology mutation test: temporarily disconnect queanbeyan → central, simulate day, then reconnect
disconnect(queanbeyan, central_molongolo);
simulate();
connect(queanbeyan, central_molongolo);

// Assertions (basic invariants)
assert(googong.volume >= 0L);
assert(dam1.volume   >= 0L);
assert(dam2.volume   >= 0L);

// Final reporting
println("--- FINAL REPORT ---");
print_report(rivers, names);
println("Peak system volume observed: " + str(max_volume(rivers)));
println("R1%=" + str(R1.percent_full()) + ", R2%=" + str(R2.percent_full()));
