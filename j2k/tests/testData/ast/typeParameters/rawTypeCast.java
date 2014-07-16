import java.util.*;

class A {
    public static Map<String, String> foo() {
        Properties props = new Properties();
        return new HashMap<>((Map)props);
    }
}