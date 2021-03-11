// RUNTIME_WITH_FULL_JDK
import java.util.*

fun test () {
    val list: LinkedList<String> = LinkedList()
    <caret>Collections.sort(list)
}