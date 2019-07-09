import java.util.*;

class A {
    private Collection<String> collection;

    A() {
        collection = createCollection();
    }

    Collection<String> createCollection() {
        return new ArrayList<>();
    }

    public void foo() {
        collection.add("1")
    }

    public Collection<String> getCollection() {
        return collection;
    }
}