// "Suppress 'REDUNDANT_NULLABLE' for statement " "true"

fun foo() {
    call("" as String?<caret>?)
}

fun call(s: String?) {}