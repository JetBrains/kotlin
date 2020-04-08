// WITH_RUNTIME
// DISABLE-ERRORS
interface T<X> {
    fun <caret>foo(x: X): X
}

enum class E : T<Int> {
    A, B, C;

    val bar = 1

    fun baz() = 2
}