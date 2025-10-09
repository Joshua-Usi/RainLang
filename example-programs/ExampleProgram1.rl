// Implements the example river system in AssignmentOne spec

// Root rivers
Body googong         = Body(5km2, 200ML);
Body jerrabombarra   = Body(3km2, 100ML);
Body upper_molongolo = Body(6km2, 250ML);

// Dams
Body dam1 = Body(1km2, 0L);
Body dam2 = Body(2km2, 0L);

// Intermediate rivers
Body queanbeyan        = Body(4km2, 50ML);
Body central_molongolo = Body(8km2, 0L);

// Output river
Body lower_molongolo = Body(10km2, 0L);

// Connections
connect(googong, dam1);
connect(dam1, queanbeyan);
connect(jerrabombarra, central_molongolo);
connect(upper_molongolo, central_molongolo);
connect(queanbeyan, central_molongolo);
connect(central_molongolo, dam2);
connect(dam2, lower_molongolo);

// Add some rainfall to kick off the simulation
rain(googong, 10mm, [40%, 30%, 20%, 10%]);
rain(jerrabombarra, 10mm, [40%, 30%, 20%, 10%]);
rain(upper_molongolo, 10mm, [40%, 30%, 20%, 10%]);

// Run the system for 10 days
simulate(10);

// Print final outflow
println(lower_molongolo.volume);
