// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String>): String? {
    OuterLoop@
    <caret>for (s in list) {
        for (line in s.lines()) {
            if (line.isBlank()) continue@OuterLoop
            return line
        }
    }
    return null
}