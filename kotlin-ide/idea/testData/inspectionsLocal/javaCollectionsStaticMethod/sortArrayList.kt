// RUNTIME_WITH_FULL_JDK
import java.util.*

fun test () {
    val list: ArrayList<Int> = ArrayList()
    <caret>Collections.sort(list)
}