import java.util.HashMap;
import java.util.Map;

class A {
    void foo() {
        Map<String, Integer> map1 = getMap1();
        Map<String, Integer> map2 = getMap2("a", 1);
    }

    <K, V> Map<K, V> getMap1() {
        return new HashMap<>();
    }

    <K, V> Map<K, V> getMap2(K k, V v) {
        HashMap<K, V> map = new HashMap<>();
        map.put(k, v);
        return map;
    }
}