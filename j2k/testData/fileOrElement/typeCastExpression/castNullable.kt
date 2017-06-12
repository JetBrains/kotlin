internal class A {
    private fun foo(o: Any?, b: Boolean): String? {
        return if (b) o as String? else ""
    }

    fun bar() {
        if (foo(null, true) == null) {
            println("null")
        }
    }
}