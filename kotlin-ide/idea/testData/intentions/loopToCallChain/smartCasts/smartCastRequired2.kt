// WITH_RUNTIME
// INTENTION_TEXT: "Replace with '...filter{}.map{}.firstOrNull()'"
// INTENTION_TEXT_2: "Replace with 'asSequence()...filter{}.map{}.firstOrNull()'"
fun foo(list: List<Any>, o: Any): Int? {
    <caret>for (s in list) {
        if (s is String && s.length > 0) {
            val x = s.length * 2
            return x
        }
    }
    return null
}