// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String?>, target: MutableList<String>) {
    <caret>for (s in list) {
        val length = s?.length ?: break
        target.add(length.toString())
    }
}