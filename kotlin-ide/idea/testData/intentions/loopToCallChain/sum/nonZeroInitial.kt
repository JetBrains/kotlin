// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'sum()'"
// IS_APPLICABLE_2: false
fun foo(list: List<Int>): Int {
    var s = 1
    <caret>for (item in list) {
        s += item
    }
    return s
}
