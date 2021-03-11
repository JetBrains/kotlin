fun foo() {
    fun bar(): (Int) -> Int = { it }
    val b = <caret>bar()
}