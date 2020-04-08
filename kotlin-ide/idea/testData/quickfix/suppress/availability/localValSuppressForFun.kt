// "Suppress 'REDUNDANT_NULLABLE' for fun foo" "true"

fun foo() {
    val a: String?<caret>? = null
}
