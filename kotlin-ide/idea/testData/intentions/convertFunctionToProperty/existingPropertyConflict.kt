// SHOULD_FAIL_WITH: Property foo already exists
class A(val n: Int) {
    fun <caret>foo(): Boolean = n > 1
}

val A.foo: Int
    get() = 1