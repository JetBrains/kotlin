import java.util.*;

class A<T>{
    void foo(Collection<String> nonMutableCollection,
            Collection<String> mutableCollection,
            Set<T> mutableSet,
            Map<String, T> mutableMap) {
        mutableCollection.addAll(nonMutableCollection);
        mutableSet.add(mutableMap.remove("a"));
    }
}