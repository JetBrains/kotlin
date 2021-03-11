// RUNTIME_WITH_FULL_JDK

import java.util.Collection;
import java.util.stream.Collectors;

class JavaCode {
    public String toJSON(Collection<Integer> collection) {
        return "[" + collection.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]";
    }
}