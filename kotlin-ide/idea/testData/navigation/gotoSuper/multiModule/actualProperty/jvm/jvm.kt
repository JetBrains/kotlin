package test

actual class Expected {
    actual fun foo() = 42

    actual val <caret>bar = "Hello"
}

// REF: [testModule_Common] (in test.Expected).bar

