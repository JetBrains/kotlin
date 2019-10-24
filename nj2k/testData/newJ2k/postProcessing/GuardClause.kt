class Test {
    fun test(s: String?) {
        requireNotNull(s) { "s should not be null" }
    }
}