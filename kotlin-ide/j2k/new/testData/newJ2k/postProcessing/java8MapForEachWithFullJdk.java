// RUNTIME_WITH_FULL_JDK

import java.util.HashMap;

class Test {
    void test(HashMap<String, String> map) {
        map.forEach((key, value) -> foo(key, value));
    }

    void foo(String key, String value) {
    }
}