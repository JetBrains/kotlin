// IS_APPLICABLE: false
class A(val n: Int) {
    fun <caret>foo(k: Int): Boolean = n - k > 1
}

fun test() {
    val t = A(1).foo(2)
}