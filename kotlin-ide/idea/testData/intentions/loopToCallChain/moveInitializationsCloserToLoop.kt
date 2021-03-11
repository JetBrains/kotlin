// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}'"
// IS_APPLICABLE_2: false
import java.util.*

fun foo(list: List<String>) {
    val result = ArrayList<Int>()

    bar()

    <caret>for (s in list) {
        result.add(s.length)
    }
}

fun bar(){}