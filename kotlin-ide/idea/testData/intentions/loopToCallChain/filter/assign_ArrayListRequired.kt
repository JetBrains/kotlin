// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterTo(){}'"
// IS_APPLICABLE_2: false
import java.util.ArrayList

fun foo(list: List<String>): ArrayList<String> {
    val result = ArrayList<String>()
    <caret>for (s in list) {
        if (s.length > 0) {
            result.add(s)
        }
    }
    return result
}