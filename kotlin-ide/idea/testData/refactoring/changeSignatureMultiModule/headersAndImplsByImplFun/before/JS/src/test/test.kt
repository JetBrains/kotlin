package test

actual fun foo() { }
actual fun <caret>foo(n: Int) { }
actual fun bar(n: Int) { }

fun test() {
    foo()
    foo(1)
    bar(1)
}