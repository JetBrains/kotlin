import java.util.HashMap

internal class A {
    fun foo() {
        val map1 = getMap1<String, Int>()
        val map2 = getMap2("a", 1)
    }

    fun <K, V> getMap1(): Map<K, V> {
        return HashMap()
    }

    fun <K, V> getMap2(k: K, v: V): Map<K, V> {
        val map = HashMap<K, V>()
        map[k] = v
        return map
    }
}