// IS_APPLICABLE: false
fun test(n: Int) {
    fun <caret>foo(): Boolean = n > 1

    val t = foo()
}