// MODULE: main
// FILE: example/JavaTypes.java
package example;

import java.util.List;
import java.util.Map;

interface Base<T> {}

public class JavaTypes<T extends CharSequence & Comparable<T>> implements Base<List<T>> {
    public T typeParameter;
    public List<String> parameterized;
    public Map<String, List<T>> nested;

    public <R extends Number & Comparable<R>> R convert(R input) {
        return input;
    }
}
