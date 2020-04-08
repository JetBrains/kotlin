// "Suppress 'REDUNDANT_NULLABLE' for statement " "true"

fun foo() {
    when ("") {
        is Any?<caret>? -> {}
    }
}