// "Suppress 'REDUNDANT_NULLABLE' for class C" "true"

class C {
    class D {
        fun foo(): String?<caret>? = null
    }
}