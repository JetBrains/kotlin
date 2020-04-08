package test

actual class Foo {
    actual val <caret>foo get() = 1
}

fun test(f: Foo) = f.foo