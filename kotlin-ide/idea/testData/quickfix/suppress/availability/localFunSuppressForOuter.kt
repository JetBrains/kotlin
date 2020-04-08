// "Suppress 'REDUNDANT_NULLABLE' for fun foo" "true"

fun foo() {
    fun local(): String?<caret>? = null
}
