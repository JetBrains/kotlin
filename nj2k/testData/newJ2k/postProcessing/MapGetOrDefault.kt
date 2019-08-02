internal class C {
    fun foo(map: Map<Int, String>): String {
        return map[1] ?: "bar"
    }
}