// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIndexed{}.firstOrNull()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filterIndexed{}.firstOrNull()'"
fun foo(list: List<String>): String? {
    var i = 0
    <caret>for (s in list) {
        if (s.length > i) {
            return s
        }
        i++
    }
    return null
}