enum class E {
    E1;

    fun test() {
        bar(<caret>Companion.foo)
    }

    fun bar(s: String) {}

    companion object {
        const val foo = ""
    }
}
