// WITH_RUNTIME
// INTENTION_TEXT: "Replace with '...filter{}.map{}.firstOrNull()'"
// INTENTION_TEXT_2: "Replace with 'asSequence()...filter{}.map{}.firstOrNull()'"
fun foo(list: List<String>, o: Any): Int? {
    if (o is Int) {
        <caret>for (s in list) {
            val length = s.length + o
            if (length > 0) {
                val x = length * o.hashCode()
                return x
            }
        }
        return null
    }
    return 0
}