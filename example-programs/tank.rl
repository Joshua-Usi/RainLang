// ---------------- "Standard library" (Automatically included in every program) ----------------
// List of all simulated bodies
Body[] __BODY_REGISTRY = [];
// List of all active rain events
__RAIN_EVENT[] __RAIN_EVENTS = [];

// The main body class exposed to the clients
class Body {
	// Used for applying volumes
	Area area;
	// Current volume, Never goes below 0
	Volume volume;
	// Total fluid added the beginning of day
	Volume sources;
	// Total fluid removed at the end of the day
	Volume sinks;
	Body[] inflows;
	Body[] outflows;
	Volume[] outflow_rates;

	Body(Area area, Volume initial_volume) {
		this.area = area;
		this.volume = initial_volume;
		this.sources = 0L;
		this.sinks = 0L;
		this.inflows = [];
		this.outflows = [];
		// The cap flow rate of outflows negative numbers mean unlimited, usually -1
		this.outflow_rates = [];

		// Bodies add themselves implicitly
		__BODY_REGISTRY.push(this);
	}

	// Apply sources
	None __apply_source() {
		this.volume = this.volume + this.sources;
	}

	// Apply sinks while also not going negative
	None __apply_sink() {
		this.volume = this.volume - this.sources;
		// Negative volume doesn't make sense so cap it
		if (this.volume < 0L) {
			this.volume = 0L;
		}
	}
}

// Internal class, Stores events about rain
class __RAIN_EVENT {
	Body body;
	Rain total_rainfall;
	Val[] kernel;
	Val day;

	__RAIN_EVENT(Body body, Rain total_rainfall, Val[] kernel) {
		this.body = body;
		this.total_rainfall = total_rainfall;
		this.kernel = kernel;
		this.day = 0;
	}

	// Apply rainfall for the current day
	None __apply() {
		// If the event expired, don't bother with applying it
		if (this.day >= this.kernel.length) {
			return;
		}
		Volume todays_inflow = this.kernel[this.day] * this.total_rainfall * this.body.area;
		this.body.volume = this.body.volume + todays_inflow;
		this.day = this.day + 1;
	}
}

// Remove a body from a system
// Used more for programmatic systems than simulations
None remove_body(Body body) {
	for (Val i = 0; i < __BODY_REGISTRY.length; i = i + 1) {
		if (body == __BODY_REGISTRY[i]) {
			__BODY_REGISTRY.removeAt(i);
			// break;
		}
	}
}

// Connect two bodies togeher with an optional flow rate
None connect(Body start, Body outflow, Volume max_flow_rate) {

}

None connect(Body start, Body outflow) {
	connect(start, outflow, -1.0L);
}

// Disconnect 2 bodies
None disconnect(Body start, Body outflow) {
	
}

None source(Body body, Volume amount) {
	body.sources = body.sources + amount;
}

None remove_source(Body body, Volume amount) {
	body.sources = body.sources - amount;
	// Below 0 is invalid, so we'll cap it
	if (body.sources < 0L) {
		body.sources = 0L;
	}
}

None sink(Body body, Volume amount) {
	body.sinks = body.sinks + amount;
}

None remove_sink(Body body, Volume amount) {
	body.sinks = body.sinks - amount;
	// Below 0 is invalid, so we'll cap it
	if (body.sinks < 0L) {
		body.sinks = 0L;
	}
}

// Rain with a custom kernel to deposit rain over a certain number of days
None rain(Body body, Rain amount, Val[] kernel) {
	// Check that the kernel does not sum to > 1
	Val sum = 0;
	for (Val i = 0; i < kernel.length; i = i + 1) {
		sum = sum + kernel[i];
	}
	assert(sum <= 1.0);
	__RAIN_EVENTS.push(__RAIN_EVENT(body, amount, kernel));
}

// With default kernel to deposit all rain in a single day
None rain(Body body, Rain amount) {
	rain(body, amount, [1]);
}

// Simulate for a certain number of days.
// Note you cannot insert events between long simulates
None simulate(Val days) {
	// Execute wavefront
	for (Val i = 0; i < days; i = i + 1) {
		// 1. Sources - Apply sources, water is added to bodies
		for (Val j = 0; j < __BODY_REGISTRY.length; j = j + 1) {
			__BODY_REGISTRY[j].__apply_source();
		}
		// 2. Rainfall - Rainfall events apply over 1 or more days according to their kernel
		for (Val j = 0; j < __RAIN_EVENTS.length; j = j + 1) {
			__RAIN_EVENTS[j].__apply();
		}
		// 3. Wavefront propagation
		// 3a. Roots - Find all Bodies with no incoming edges. These are considered root bodies and their flows are computed first
		// 3b. Propagation - Water flows downstream in topological order. A body is only evaluated once all upstream inflows are finished computing
		// 3c. Split Rules - If a river splits, outflows divide euqally among children, with respect to flow limits
		// 3d. Merge Rules - If multiple rivers flow into a node, inflows accumulate before propagation continues
		// 3e. Equalisation - Connected bodies attempt to minimise the differences in their volumes, constrainted by flow limits
		// 4. Sinks - Apply all sinks. Water is removed from all water bodies with sinks
		for (Val j = 0; j < __BODY_REGISTRY.length; j = j + 1) {
			__BODY_REGISTRY[j].__apply_sink();
		}
		// Cleanup expired rain events
		for (Val i = __RAIN_EVENTS.length - 1; i >= 0; i = i - 1) {
			__RAIN_EVENT e = __RAIN_EVENTS[i];
			if (e.day >= e.kernel.length) {
				__RAIN_EVENTS.removeAt(i);
			}
		}
	}
}

// Overload to just simulate 1 day
None simulate() {
	simulate(1);
}

// Prints a nice hyrology report
None hydrology_report() {
	print("--------------------------------------------------------------------------------------------------------------------------------");
	print("| Hydrology Report                                                                                                             |");
	print("--------------------------------------------------------------------------------------------------------------------------------");
}

// ---------------- Start of actual program ----------------

// Define 2 bodies, one filled, one empty
Body a = Body(10m2, 100L);
Body b = Body(10m2, 0L);

// Connect the two of them with an unlimited flow rate
connect(a, b);

// Define that it'll rain 10mm on a over 4 days
// 4mm on day 1, 3mm on day 2, 2mm on day 3, 1mm on day 4, simulates runoff over time
rain(a, 10mm, [ 40%, 30%, 20%, 10% ]);

simulate();

print("A details: ");
print("Area: " + a.area);
print("Volume: " + a.volume);

print("B details: ");
print("Area: " + b.area);
print("Volume: " + b.volume);

// ---------------- Automatically added to the end of programs (if a hydrology report wasn't already requested) ----------------

// Print a nicely formatted report on program exit
hydrology_report();