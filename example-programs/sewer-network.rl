// ------------------------------------------------------------
// Sewer network generator (houses → streets → mains → sea)
// - Topology: tree-like DAG
// - Dynamics: sources at houses, optional rainfall kernels
// - Flow: houses -> street_junctions -> mains -> sea (sink)
// ------------------------------------------------------------

// ---- Tunable parameters ------------------------------------
Val HOUSES_PER_STREET = 20;
Val STREETS_PER_MAIN  = 10;
Val NUM_MAINS         = 2;     // total streets = STREETS_PER_MAIN * NUM_MAINS
Val HOUSE_COUNT       = HOUSES_PER_STREET * STREETS_PER_MAIN * NUM_MAINS;

// Per-edge flow limits (pipe capacity) — optional
Val HOUSE_PIPE_LIMIT  = 2000l;   // L/day equivalent capacity from house to street
Val STREET_PIPE_LIMIT = 10000l;  // from street to main
Val MAIN_PIPE_LIMIT   = 100000l; // from main to sea

// House source (daily wastewater / runoff)
// You can interpret this as daily average volume added at each house
Val HOUSE_DAILY_SOURCE = 300l;

// Optional rainfall kernel (3-day response), applied to streets (impervious area)
Val USE_RAINFALL = true;

// ---- Node (body) factories --------------------------------
// Bodies represent junctions/pipes/tanks; area used if rain() is applied.
// Initial storage 0 L; areas chosen to be modest but non-zero.
Body mkHouse()  { return Body(50m2, 0l); }
Body mkStreet() { return Body(200m2, 0l); }
Body mkMain()   { return Body(500m2, 0l); }
Body mkSea()    { return Body(1_000_000m2, 0l); } // big sink boundary

// ---- Arrays for nodes -------------------------------------
Body houses[HOUSE_COUNT];
Body streets[STREETS_PER_MAIN * NUM_MAINS];
Body mains[NUM_MAINS];
Body sea;

// ---- Index helpers ----------------------------------------
Val streetIndex(Val houseIdx) {
    // group houses into blocks of HOUSES_PER_STREET
    return floor(houseIdx / HOUSES_PER_STREET);
}
Val mainIndex(Val streetIdx) {
    // group streets into blocks of STREETS_PER_MAIN
    return floor(streetIdx / STREETS_PER_MAIN);
}

// ---- Build topology ---------------------------------------
Void buildNetwork() {
    // Create all nodes
    for (Val i = 0; i < HOUSE_COUNT; i = i + 1) {
        houses[i] = mkHouse();
    }
    for (Val s = 0; s < (STREETS_PER_MAIN * NUM_MAINS); s = s + 1) {
        streets[s] = mkStreet();
    }
    for (Val m = 0; m < NUM_MAINS; m = m + 1) {
        mains[m] = mkMain();
    }
    sea = mkSea();
    sink(sea); // designate as outlet boundary

    // Connect: house -> street (with pipe limits)
    for (Val i = 0; i < HOUSE_COUNT; i = i + 1) {
        Val s = streetIndex(i);
        // connect(source, target, optional_flow_limit)
        connect(houses[i], streets[s], HOUSE_PIPE_LIMIT);
    }

    // Connect: street -> main
    for (Val s = 0; s < (STREETS_PER_MAIN * NUM_MAINS); s = s + 1) {
        Val m = mainIndex(s);
        connect(streets[s], mains[m], STREET_PIPE_LIMIT);
    }

    // Connect: main -> sea
    for (Val m = 0; m < NUM_MAINS; m = m + 1) {
        connect(mains[m], sea, MAIN_PIPE_LIMIT);
    }
}

// ---- Inflows ----------------------------------------------
Void addHouseSources() {
    for (Val i = 0; i < HOUSE_COUNT; i = i + 1) {
        // External inflow boundary: daily household discharge
        source(houses[i], HOUSE_DAILY_SOURCE);
    }
}

Void addStreetRainIfEnabled() {
    if (USE_RAINFALL) {
        // A short, peaky 3-day kernel (flashy urban runoff feel)
        // Day fractions sum to 100% (interpretable as intensity or delayed response)
        Val kernel[3] = [70%, 20%, 10%];
        // Apply a uniform rainfall event of 5 mm across all street catchments
        for (Val s = 0; s < (STREETS_PER_MAIN * NUM_MAINS); s = s + 1) {
            rain(streets[s], 5mm, kernel);
        }
    }
}

// ---- Simulation driver ------------------------------------
Void runScenario() {
    buildNetwork();
    addHouseSources();
    addStreetRainIfEnabled();

    // Example dynamic control: open a diversion from main[0] to a spillway only on day 4+
    // (Shows you can event-drive connectivity)
    // We'll pre-create a spillway sink but only connect it later.
    Body spill = Body(1000m2, 0l);
    sink(spill);

    // Simulate 10 days with a structural change after day 3
    for (Val day = 1; day <= 10; day = day + 1) {
        if (day == 4) {
            // Allow emergency relief from main[0] directly to spill after day 3
            connect(mains[0], spill, 50000l);
        }
        simulate(1); // advance one day per loop
    }
}

// ---- Kick off ---------------------------------------------
runScenario();
