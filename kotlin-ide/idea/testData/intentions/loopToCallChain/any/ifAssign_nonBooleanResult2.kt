// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String>) {
    var result = takeInt()
    <caret>for (s in list) {
        if (s.length > 0) {
            result = 1
            break
        }
    }
}

fun takeInt(): Int = 0