// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

actual abstract class Base {
    actual abstract fun foo(param: String): Int
}

actual class <caret>My : Base() {
    actual override fun foo(param: String) = param.length
}