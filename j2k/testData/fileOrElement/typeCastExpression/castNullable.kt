internal class A {
    private fun foo(o: Any?, b: Boolean): String? {
        if (b) return o as String?
        return ""
    }

    fun bar() {
        if (foo(null, true) == null) {
            println("null")
        }
    }
}