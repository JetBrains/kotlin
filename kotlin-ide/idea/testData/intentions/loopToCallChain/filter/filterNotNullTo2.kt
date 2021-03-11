// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterNotNullTo()'"
// IS_APPLICABLE_2: false
import java.util.ArrayList

fun foo(list: List<String?>) {
    val target = ArrayList<String>(1000)
    <caret>for (s in list) {
        if (s != null) {
            target.add(s)
        }
    }
}