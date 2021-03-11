// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}.toSet()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().map{}.toSet()'"
import java.util.HashSet

fun foo(map: Map<Int, String>): Collection<Int> {
    val result = HashSet<Int>()
    <caret>for (s in map.values) {
        result.add(s.length)
    }
    return result
}