// MODULE: main
// FILE: example/JavaTypes.java
package example;

import java.util.List;

public class JavaTypes<T> {
    public List<?> unbounded;
    public List<? extends T> upperBound;
    public List<? super String> lowerBound;
    public List raw;
}
