import java.util.*;

class A {
    void foo(Set<String> set) {
        bar(set);
    }

    void bar(Collection<String> collection) {
        collection.add("a");
    }
}