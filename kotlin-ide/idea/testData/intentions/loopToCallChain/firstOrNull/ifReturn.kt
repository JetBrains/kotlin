// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'firstOrNull{}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>): String? {
    <caret>for (s in list) {
        if (s.length > 0) {
            return s
        }
    }
    return null
}