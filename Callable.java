import java.util.*;

interface Callable {
    int arity();
    Object call(Interpreter interpreter, Token paren, List<Object> args);
}