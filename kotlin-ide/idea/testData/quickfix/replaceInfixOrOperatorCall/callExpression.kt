// "Replace with safe (?.) call" "true"
// WITH_RUNTIME

fun foo() {}

fun bar() {
    val fff: (() -> Unit)? = ::foo
    <caret>fff()
}