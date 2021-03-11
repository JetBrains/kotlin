// WITH_RUNTIME
// DISABLE-ERRORS
interface T<X> {
    val <caret>foo: X
}

enum class E : T<Int> {
    A, B, C;

    val bar = 1

    fun baz() = 2
}