// "Wrap with '?.let { ... }' call" "true"
// WITH_RUNTIME

fun foo(exec: (() -> Unit)?) {
    <caret>exec()
}