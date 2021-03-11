// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'toList()'"
// IS_APPLICABLE_2: false
fun foo(map: Map<Int, String>): List<String> {
    val result = arrayListOf<String>()
    <caret>for (s in map.values) {
        result.add(s)
    }
    return result
}