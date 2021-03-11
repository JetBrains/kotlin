// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'contains()'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>) {
    var v = false
    <caret>for (s in list) {
        if (s == "a") {
            v = true
        }
    }
}