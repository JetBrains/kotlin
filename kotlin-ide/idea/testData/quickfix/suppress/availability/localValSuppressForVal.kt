// "Suppress 'REDUNDANT_NULLABLE' for val a" "true"

fun foo() {
    val a: String?<caret>? = null
}
