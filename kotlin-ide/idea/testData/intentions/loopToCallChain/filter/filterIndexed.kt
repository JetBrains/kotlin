// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIndexed{}.firstOrNull()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filterIndexed{}.firstOrNull()'"
fun foo(list: List<String>): String? {
    <caret>for ((index, s) in list.withIndex()) {
        if (s.length > index) {
            return s
        }
    }
    return null
}