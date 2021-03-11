// WITH_RUNTIME
class A {
    fun <caret>String.foo(n: Int): Boolean {
        return length - n/2 > 1
    }

    fun test() {
        "1".foo(2)
    }
}

fun test() {
    with(A()) {
        "1".foo(2)
    }
}