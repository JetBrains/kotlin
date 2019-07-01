import java.util.HashMap

internal class Test {
    fun test(map: HashMap<String, String>) {
        map.forEach { (key, value) -> foo(key, value) }
    }

    fun foo(key: String, value: String) {}
}