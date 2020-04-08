// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String>): String? {
    var i = 1
    <caret>for (s in list) {
        if (s.length > i) {
            return s
        }
        i++
    }
    return null
}