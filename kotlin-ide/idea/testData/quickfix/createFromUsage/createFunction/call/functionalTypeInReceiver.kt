// "Create extension function '((Int) -> String).bar'" "true"

fun foo(block: (Int) -> String) {
    block.b<caret>ar()
}