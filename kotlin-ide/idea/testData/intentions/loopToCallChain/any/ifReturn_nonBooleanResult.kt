// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'any{}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>): Int {
    <caret>for (s in list) {
        if (s.length > 0) {
            return -1
        }
    }
    return takeInt()
}

fun takeInt(): Int = 0