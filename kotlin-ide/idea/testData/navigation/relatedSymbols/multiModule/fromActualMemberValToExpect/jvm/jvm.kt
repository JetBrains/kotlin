package test

actual class Foo {
    actual val <caret>bar: Int get() = 1
}

// REF: [testModule_Common] (in test.Foo).bar