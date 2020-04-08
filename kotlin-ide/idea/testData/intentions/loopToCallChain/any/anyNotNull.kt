// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'any{}'"
// IS_APPLICABLE_2: false
fun foo(list: List<Any?>): Boolean {
    <caret>for (a in list) {
        if (a != null) return true
    }
    return false
}