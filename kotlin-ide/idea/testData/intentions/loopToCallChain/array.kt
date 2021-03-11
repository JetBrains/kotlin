// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'firstOrNull{}'"
// IS_APPLICABLE_2: false
fun foo(array: Array<String>): String? {
    <caret>for (s in array) {
        if (s.isNotBlank()) {
            return s
        }
    }
    return null
}
