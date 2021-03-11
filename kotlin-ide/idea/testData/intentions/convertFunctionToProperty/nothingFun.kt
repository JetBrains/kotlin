// WITH_RUNTIME
// IS_APPLICABLE: false
class A(val n: Int) {
    fun <caret>foo(): Nothing = throw Exception("foo")
}

fun test() {
    A(1).foo()
}