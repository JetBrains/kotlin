// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<Any>): String? {
    var v: String? = null
    <caret>for (o in list) {
        if (bar(o as String)) {
            v = o
            break
        }
    }
    return v
}

fun bar(s: String): Boolean = true