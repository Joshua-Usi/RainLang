Refer to [[Types]] for the typing system
# connect(Body a, Body b, optional: Volume flow_limit)
Adds a connection (Directional) between two bodies
### Params
- `Body a` The source body
- `Body b` The sink body
- `Volume flow_limit` The maximum flow rate of this connection, defaults to infinity
Returns: `None`
# disconnect(Body a, Body b)
Removes a connection (If it exists) from two bodies
### Params
- `Body a` The source body
- `Body b` The sink body
Returns: `None`
# source(Body a, Volume v)
Adds water without question to a body
### Params
- `Body a` The applicable body
- `Volume v` The volume of water added per day
Returns: `None`
# sink(Body a, Volume v)
Removes water without question from a body
### Params
- `Body a` The applicable body
- `Volume v` The volume of water removed per day
Returns: `None`
# rain(Body a, Rain rate, optional: Val[] kernel)
Denotes a rain event to occur over a specific body
### Params
- `Body a` The applicable body
- `Rain rate` The total amount of rain to apply
- `Val[] kernel` The rain kernel to use (Rain applied over 2 or more days), defaults to `[ 100% ]`
Returns: `None`
# simulate(optional: Val days)
Runs hydrology simulation for 1 day
### Params
- `Val days` the number of days to simulate, defaults to `1`
Returns: `None`
# print(Val|Volume|Area|Rain|String s)
Prints out the given string without a new line appended at the end
### Params
- `Val|Volume|Area|Rain|String s` The string to print out. Non string types are implicitly stringified
Returns: `None`
# println(Val|Volume|Area|Rain|String s)
Prints out the given string with a new line appended at the end
### Params
- `Val|Volume|Area|Rain|String s` The string to print out. Non string types are implicitly stringified
Returns: `None`
# str(Val|Volume|Area|Rain s)
Converts a value to a string
### Params
- `Val|Volume|Area|Rain s` The value to be converted to a string
Returns: `String`
# assert(Bool expression)
Asserts an expression is true, if so, the program continues as normal, if not, the program crashes
### Params
- `Bool expression` The expression to evaluate
Returns: `String`