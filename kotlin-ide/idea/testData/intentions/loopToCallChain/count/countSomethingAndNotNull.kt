// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'count{}'"
// IS_APPLICABLE_2: false
fun f(list: List<Any?>): Int{
    var c = 0
    <caret>for (d in list) {
        if (d == "") continue
        if (d != null) {
            if (d is Int) continue
            c++
        }
    }
    return c
}