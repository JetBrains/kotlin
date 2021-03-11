fun foo(x: Int) {}

fun bar() {
    foo(if (<caret>false) 1 else 2)
}