// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'sum()'"
// IS_APPLICABLE_2: false
fun foo(list: List<Byte>): Int {
    var s = 0
    <caret>for (item in list) {
        s += item
    }
    return s
}
