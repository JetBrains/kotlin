import kotlin.text.Regex
import kotlin.test.*

var inlinesCount = 0

fun exception() {
    error("FAIL!")
}

fun main() {
    try {
        exception()
    }
    catch (e:Exception) {
        val stackTrace = e.getStackTrace()
        inlinesCount = stackTrace.count { it.contains("[inlined]")}
        stackTrace.take(6).forEach(::checkFrame)
    }
    println(inlinesCount)
}
internal val regex = Regex("^(\\d+)\\ +.*/(.*):(\\d+):.*$")
internal fun checkFrame(value:String) {
    val goldValues = arrayOf<Pair<String, Int>?>(
            null,
            null,
            null,
            null,
            *(if (inlinesCount != 0) arrayOf(null) else emptyArray()),
            "stack_trace_inline.kt" to 7,
            "stack_trace_inline.kt" to 12)
    val (pos, file, line) = regex.find(value)!!.destructured
    goldValues[pos.toInt()]?.let {
        assertEquals(it.first, file)
        assertEquals(it.second, line.toInt())
    }
}