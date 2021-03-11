// WITH_RUNTIME
// IS_APPLICABLE: false
class A(val n: Int) {
    fun <caret>foo() {
        println(n)
    }
}

fun test() {
    A(1).foo()
}