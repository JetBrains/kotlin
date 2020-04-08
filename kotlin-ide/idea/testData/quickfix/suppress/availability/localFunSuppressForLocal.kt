// "Suppress 'REDUNDANT_NULLABLE' for fun local" "true"

fun foo() {
    fun local(): String?<caret>? = null
}
