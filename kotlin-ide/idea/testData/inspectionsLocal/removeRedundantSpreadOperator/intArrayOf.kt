fun foo(vararg x: Int) {}

fun bar() {
    foo(<caret>*intArrayOf(1))
}
