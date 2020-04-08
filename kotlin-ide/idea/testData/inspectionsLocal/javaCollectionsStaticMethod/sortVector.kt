// RUNTIME_WITH_FULL_JDK
import java.util.*

fun test () {
    val list: Vector<String> = Vector()
    <caret>Collections.sort(list)
}