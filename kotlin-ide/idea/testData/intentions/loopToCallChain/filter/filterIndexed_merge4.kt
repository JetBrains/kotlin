// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIndexed{}.firstOrNull()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filterIndexed{}.firstOrNull()'"
fun foo(list: List<String>): String? {
    var index2 = 0
    <caret>for ((index1, s) in list.withIndex()) {
        if (s.length > index1) continue
        if (s.length < index2++) continue
        return s
    }
    return null
}