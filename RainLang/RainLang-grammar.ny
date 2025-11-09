program → statement* EOF ;

// Generalised statement
statement → declaration | return_stmt | control_flow | expression_stmt | block ;

// Standalone expressions
expression_stmt → expression ";" ;

// General declarations
// Semi-colon here so that we can reuse for for-loops
declaration → (variable_decl ";") | function_decl | class_decl ;
// Support arrays, we only support 1D arrays
type → (IDENTIFIER ("[" "]")?) | NONE ;

// Variable declarations
variable_decl → type IDENTIFIER "=" expression ;

// Class declarations
class_decl       → CLASS IDENTIFIER "{" class_member* "}" ;
class_member     → field_decl | function_decl | constructor_decl ;
field_decl       → type identifier ("=" expression)? ";" ;
constructor_decl → identifier "(" param_list? ")" block ;

// Functions
function_decl → type IDENTIFIER "(" param_list? ")" block ;
param_list    → param ("," param)* ;
param         → type IDENTIFIER ;
return_stmt   → RETURN expression? ";" ;

// Control flow
control_flow → if_stmt | while_stmt | for_stmt ;
if_stmt      → IF "(" expression ")" block ( ELSE ( if_stmt | block ) )? ;
while_stmt   → WHILE "(" expression ")" block ;
for_stmt     → FOR "(" (variable_decl | expression)? ";" expression? ";" expression? ")" block ;

// General purpose block for scoping and closures + blocks that can be used for function bodies and control flow bodies
block → "{" statement* "}" ;

// Literals
literal → NUMBER | STRING | TRUE | FALSE | NONE | array_lit ;
array_lit → "[" (expression ("," expression)*)? "]" ;

// Expressions
expression → assignment ;

// Assignment (right-associative)
// Restrict LHS to lvalues
assignment → lvalue "=" assignment | logical_or ;
lvalue     → IDENTIFIER lvalue_tail* ;
lvalue_tail → "." IDENTIFIER | "[" expression "]" ;

// Short-circuit logic
logical_or  → logical_and ("||" logical_and)* ;
logical_and → equality ("&&" equality)* ;

// Comparisons
equality   → relational (("==" | "!=") relational)? ;
relational → additive (("<" | "<=" | ">" | ">=") additive)? ;

// Arithmetic
additive       → multiplicative (("+" | "-") multiplicative)* ;
multiplicative → unary (("*" | "/") unary)* ;

// Unary
unary     → ( "!" | "+" | "-" ) unary | postfix ;

// Supports inline function call, inline object creation, array indexing and object property access
postfix → primary (postop)* ;
postop  → "(" argument_list? ")" | "[" expression "]" | "." IDENTIFIER ;

// Primaries and calls
primary       → literal | IDENTIFIER | THIS | "(" expression ")" ;
argument_list → expression ("," expression)* ;
