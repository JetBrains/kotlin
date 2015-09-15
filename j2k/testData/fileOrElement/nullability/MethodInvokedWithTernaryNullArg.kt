internal class C {
    private fun foo(s: String?) {
    }

    internal fun bar(b: Boolean) {
        foo(if (b) "a" else null)
    }
}