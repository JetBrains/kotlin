// "Suppress 'REDUNDANT_NULLABLE' for enum entry A" "true"

enum class E {
    A {
        fun foo(): String??<caret> = null
    }
}