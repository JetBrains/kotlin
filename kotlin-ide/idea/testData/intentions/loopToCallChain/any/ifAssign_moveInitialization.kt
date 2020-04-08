// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'any{}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>, p: Int) {
    var found: Boolean
    if (p > 0) {
        found = false
        println("Starting the search")
        <caret>for (s in list) {
            if (s.length > 0) {
                found = true
                break
            }
        }
    }
    else {
        found = true
    }
}