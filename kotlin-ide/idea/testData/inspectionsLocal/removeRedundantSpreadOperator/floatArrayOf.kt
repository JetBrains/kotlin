fun foo(vararg x: Float) {}

fun bar() {
    foo(<caret>*floatArrayOf(1.0f))
}
