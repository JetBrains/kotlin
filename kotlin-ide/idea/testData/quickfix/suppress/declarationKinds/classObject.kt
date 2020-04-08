// "Suppress 'REDUNDANT_NULLABLE' for companion object Companion of C" "true"

class C {
    companion object {
        var foo: String?<caret>? = null
    }
}