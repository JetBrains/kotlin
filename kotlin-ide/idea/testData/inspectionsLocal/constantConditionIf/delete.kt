fun foo(x: Int) {}

fun bar() {
    if (<caret>false) {
        foo(1)
        foo(2)
    }
}