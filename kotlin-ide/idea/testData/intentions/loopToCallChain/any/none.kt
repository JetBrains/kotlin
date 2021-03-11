// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'none{}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>): Boolean {
    <caret>for (s in list) {
        if (s.length > 0) {
            return false
        }
    }
    return true
}