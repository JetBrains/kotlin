// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String>): Int? {
    <caret>for (s in list) {
        var length = s.length
        if (length > 0) return ++length
    }
    return null
}