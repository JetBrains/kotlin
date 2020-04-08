fun foo(s: String) {}

fun bar() {
    if (<caret>true) {
        foo("a")
        foo("b")
    } else 2
}