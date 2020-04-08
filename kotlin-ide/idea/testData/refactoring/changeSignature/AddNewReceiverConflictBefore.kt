fun length(): Int = 1

fun <caret>foo(k: Int): Boolean {
    return length() - k > 0
}

class X(val k: Int) {
    fun length() = 2
}

fun test() {
    foo(2)
}