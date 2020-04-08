// "Surround with null check" "true"

fun foo(exec: (() -> Unit)?) {
    <caret>exec()
}