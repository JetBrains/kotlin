// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'flatMap{}.firstOrNull{}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().flatMap{}.firstOrNull{}'"
fun foo(list: List<String>): String? {
    var result: String? = null
    MainLoop@
    <caret>for (s in list) {
        for (line in s.lines()) {
            if (line.isNotBlank()) {
                result = line
                break@MainLoop
            }
        }
    }
    return result
}