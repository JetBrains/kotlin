// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'any{}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>, it: Int) {
    var found = false
    <caret>for (s in list) {
        if (s.length > it) {
            found = true
            break
        }
    }
}