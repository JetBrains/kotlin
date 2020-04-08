// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filter{}'"
// IS_APPLICABLE_2: false
import java.util.ArrayList

fun foo(): List<String> {
    while (true) {
        val result = ArrayList<String>()
        <caret>for (s in list()) {
            if (s.length > 0) {
                result.add(s)
            }
        }

        if (bar1()) continue
        if (bar2()) break

        return result
    }

    return emptyList()
}

fun list() = listOf("a")
fun bar1() = true
fun bar2() = true