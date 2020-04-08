// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filter{}'"
// IS_APPLICABLE_2: false
import java.util.ArrayList

fun foo(array: IntArray): List<Int> {
    val result = ArrayList<Int>()
    <caret>for (i in array) {
        if (i % 3 == 0) {
            result.add(i)
        }
    }
    return result
}
