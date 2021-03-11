package test

actual fun foo() { }
actual fun baz(n: Int) { }
actual fun bar(n: Int) { }

fun test() {
    foo()
    baz(1)
    bar(1)
}