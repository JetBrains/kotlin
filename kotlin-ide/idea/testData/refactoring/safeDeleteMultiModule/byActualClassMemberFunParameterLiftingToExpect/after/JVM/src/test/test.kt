package test

actual class Foo {
    actual fun foo() {
        n + 1
    }
}

fun test(f: Foo) {
    f.foo()
    f.foo()
}