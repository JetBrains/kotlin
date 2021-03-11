// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

class A {
    class M
    actual fun <caret>a(): M = TODO()
}