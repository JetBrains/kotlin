class C {
    private fun foo(s: String?) {
    }

    fun bar(b: Boolean) {
        foo(if (b) "a" else null)
    }
}