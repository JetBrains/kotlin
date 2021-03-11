// "Create expected enum class in common module testModule_Common" "true"
// DISABLE-ERRORS

actual enum class <caret>Complex {
    FIRST {
        override fun foo() {}
    };

    actual abstract fun foo()
}