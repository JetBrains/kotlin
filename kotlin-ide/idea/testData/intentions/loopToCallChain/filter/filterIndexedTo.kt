// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIndexedTo(){}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>, target: MutableList<String>) {
    <caret>for ((index, s) in list.withIndex()) {
        if (s.length > index)
            target.add(s)
    }
}