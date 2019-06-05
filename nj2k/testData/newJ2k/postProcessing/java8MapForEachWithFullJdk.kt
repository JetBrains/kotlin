internal class Test {
    fun test(map: HashMap<String, String>) {
        map.forEach { (key: String, value: String) -> foo(key, value) }
    }

    fun foo(key: String, value: String) {}
}