package test

actual class Foo {
    actual val foo get() = 1
}

fun test(f: Foo) = f.foo