// "Suppress 'REDUNDANT_NULLABLE' for fun foo" "true"

class C {
    class D {
        fun foo(): String?<caret>? = null
    }
}