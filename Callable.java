import java.util.*;

interface Callable {
    int arity();
    Object call(Interpreter interpreter, List<Object> args);
}