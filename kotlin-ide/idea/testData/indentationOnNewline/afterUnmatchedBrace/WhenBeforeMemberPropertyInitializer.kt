class Test {
    val test = when {<caret>foo()

    fun foo(): Int {
        return 42
    }
}
