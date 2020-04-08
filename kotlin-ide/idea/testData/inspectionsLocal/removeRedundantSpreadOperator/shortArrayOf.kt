fun foo(vararg x: Short) {}

fun bar() {
    foo(<caret>*shortArrayOf(1))
}
