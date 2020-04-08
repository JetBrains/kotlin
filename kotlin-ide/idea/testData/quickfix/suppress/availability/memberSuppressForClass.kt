// "Suppress 'REDUNDANT_NULLABLE' for class C" "true"

class C {
    fun foo(): String?<caret>? = null
}