// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filter{}.mapTo(){}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filter{}.mapTo(){}'"
import java.util.ArrayList

fun foo(list: List<String>): List<Int> {
    val target = ArrayList<Int>(100)
    <caret>for (s in list) {
        if (s.length > 0)
            target.add(s.hashCode())
    }
    return target
}
