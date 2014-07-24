//file
import java.util.*;

class A {
    Map<String, String> foo() {
        List<String> list1 = Collections.emptyList();
        List<Integer> list2 = Collections.singletonList(1);
        Set<String> set1 = Collections.emptySet();
        Set<String> set2 = Collections.singleton("a");
        return Collections.emptyMap();
    }
}