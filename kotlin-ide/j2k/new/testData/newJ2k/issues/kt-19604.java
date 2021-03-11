// RUNTIME_WITH_FULL_JDK

import java.util.HashMap;

public class TestMethodReference {
    private HashMap<String, String> hashMap = new HashMap<>();

    public void update(String key, String msg) {
        hashMap.merge(key, msg, String::concat);
    }
}