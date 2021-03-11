// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filter{}'"
// IS_APPLICABLE_2: false
import java.util.*

fun foo(): List<Int> {
    val result = ArrayList<Int>()
    <caret>for (i in 1..10) {
        if (i % 3 == 0) {
            result.add(i)
        }
    }
    return result
}
