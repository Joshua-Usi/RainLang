// Define 5 bodies, one filled, the rest empty
Body a = Body("Mountain Spring", 10m2, 100L);
Body b = Body("West River", 10m2, 0L);
Body c = Body("North Creek", 10m2, 0L);
Body d = Body("East Equestiary", 10m2, 0L);
Body e = Body("Ocean Outlet", 10m2, 0L);

// Connect them with unlimited flow rate
connect(a, b);
connect(b, c);
connect(c, d);
connect(d, e);

// Define that it'll rain 10mm on c over 4 days
// 4mm on day 1, 3mm on day 2, 2mm on day 3, 1mm on day 4, simulates runoff over time
// rain(c, 10mm, [ 40%, 30%, 20%, 10% ]);

// 10L of water enters a each day
// source(a, 10L);
// 10L of water leaves the system from e each day
sink(e, 10L);

// Simulate for this many days
Val days_to_simulate = 10;

simulate(days_to_simulate);

hydrology_report();