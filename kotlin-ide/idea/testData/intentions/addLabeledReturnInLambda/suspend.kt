// IS_APPLICABLE: false
// WITH_RUNTIME

fun foo() {
    suspend {
        <caret>true
    }
}