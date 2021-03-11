// WITH_RUNTIME
// IS_APPLICABLE: FALSE

fun foo(): Boolean {
    listOf(1,2,3).find {
        return <caret>true
    }
    return false
}