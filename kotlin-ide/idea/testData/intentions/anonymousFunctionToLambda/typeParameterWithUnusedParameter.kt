fun <T> foo(fn: (T) -> Unit) {}

fun test() {
    foo(<caret>fun(x: String) {
    })
}