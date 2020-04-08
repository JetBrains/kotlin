// IS_APPLICABLE: false
// WITH_RUNTIME
fun foo(): Boolean {
    return <caret>("" ?: return false) in listOf("")
}