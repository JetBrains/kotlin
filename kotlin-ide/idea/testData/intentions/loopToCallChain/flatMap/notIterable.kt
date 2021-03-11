// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String>): Char? {
    <caret>for (s in list) {
        for (c in s) {
            if (c != ' ') return c
        }
    }
    return null
}