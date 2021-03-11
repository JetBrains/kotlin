// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filter{}.forEach{}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filter{}.forEach{}'"
fun foo(list: List<String>): Long {
    var count = 0L
    <caret>for (s in list) {
        if (s.length > 10) {
            count++
        }
    }
    return count
}