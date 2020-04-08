// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIndexed{}.map{}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filterIndexed{}.map{}.toList()'"
import java.util.*

fun foo(list: List<String>): List<Int> {
    val result = ArrayList<Int>()
    var i = 0
    <caret>for (s in list) {
        if (s.length > i) {
            result.add(s.length)
        }
        i++
    }
    return result
}