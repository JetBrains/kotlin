// RUNTIME_WITH_FULL_JDK

import java.util.Map;

class C {
    private String foo(Map<Integer, String> map) {
        return map.getOrDefault(1, "bar");
    }
}