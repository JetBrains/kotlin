import java.util.*;

class A {
    void foo(Collection<String> collection) {
        bar(collection);
    }

    void bar(Collection<String> collection) {
        if (collection.size() < 5) {
            foo(collection);
        }
        else {
            collection.add("a")
        }
    }
}