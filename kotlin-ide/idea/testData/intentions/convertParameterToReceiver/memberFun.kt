// WITH_RUNTIME
class A {
    fun foo(<caret>s: String, n: Int): Boolean {
        return s.length - n/2 > 1
    }

    fun test() {
        foo("1", 2)
    }
}

fun test() {
    with(A()) {
        foo("1", 2)
    }
}