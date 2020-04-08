// WITH_RUNTIME
// FIX: Convert to 'also'

fun foo() {
    "".<caret>apply {
        ->
        println(this)
    }
}
