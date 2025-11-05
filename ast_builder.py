DIRECTORY = "Expr.java"

TYPES = [
    "Binary      : Expr left, Token operator, Expr right",
    "Grouping    : Expr expression",
    "Literal     : Object value, Token unit",
    "Unary       : Token operator, Expr right",
    "Variable    : Token name",
    "Assign      : Token name, Expr value",
    "Logical     : Expr left, Token operator, Expr right",
    "Call        : Expr callee, Token paren, List<Expr> arguments",
    "Get         : Expr object, Token name",               # obj.name
    "Set         : Expr object, Token name, Expr value",   # obj.name = value
    "Index       : Expr array, Expr index, Token bracket",               # arr[expr]
    "IndexSet    : Expr array, Expr index, Expr value, Token bracket",    # arr[expr] = value
    "Array       : List<Expr> elements",
    "This        : Token keyword"
]


def define_type(f, base_name: str, class_name: str, field_list: str):
    f.write(f"  static class {class_name} extends {base_name} {{\n")
    # Constructor
    f.write(f"    {class_name}({field_list}) {{\n")
    fields = [part.strip() for part in field_list.split(",")]
    for field in fields:
        name = field.split()[1]
        f.write(f"      this.{name} = {name};\n")
    f.write("    }\n\n")
    # Visitor
    f.write("\n")
    f.write("    @Override\n")
    f.write("    <R> R accept(Visitor<R> visitor) {\n")
    f.write(f"      return visitor.visit{class_name}{base_name}(this);\n")
    f.write("    }\n")
    # Fields
    for field in fields:
        f.write(f"    final {field};\n")
    f.write("  }\n")

def define_visitor(f, base_name: str, types: list[str]):
    f.write("  interface Visitor<R> {\n")

    for type_def in types:
        type_name = type_def.split(":")[0].strip()
        f.write(
            f"    R visit{type_name}{base_name}("
            f"{type_name} {base_name.lower()});\n"
        )

    f.write("  }\n\n")

def main():
    with open(DIRECTORY, "w", encoding="utf-8") as f:
        f.write("""import java.util.List;

abstract class Expr {
""")
        define_visitor(f, "Expr", TYPES);
        # The AST classes
        for type_def in TYPES:
            class_name, field_list = [s.strip() for s in type_def.split(":", 1)]
            define_type(f, "Expr", class_name, field_list)
            f.write("\n")
        f.write("\n  abstract <R> R accept(Visitor<R> visitor);\n")
        f.write("}")
        
if __name__ == "__main__":
    main()
