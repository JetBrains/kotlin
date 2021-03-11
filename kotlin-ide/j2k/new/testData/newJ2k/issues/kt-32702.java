// RUNTIME_WITH_FULL_JDK

import java.util.*;

class Test {
    public void context() {
        List<Double> items = new ArrayList<>();
        items.add(1.0);
        items.forEach(System.out::println);
    }
}