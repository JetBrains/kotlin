// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterTo(){}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>, target: MutableList<String>) {
    <caret>for (s in list) {
        if (s.length > 0)
            target.add(s)
    }
}