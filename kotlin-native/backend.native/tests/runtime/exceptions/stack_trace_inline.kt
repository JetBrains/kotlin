import kotlin.text.Regex
import kotlin.test.*

fun exception() {
    error("FAIL!")
}

fun main() {
    try {
        exception()
    }
    catch (e:Exception) {
        val stackTrace = e.getStackTrace()
        stackTrace.take(6).forEach(::checkFrame)
    }
}
internal val regex = Regex("^(\\d+)\\ +.*/(.*):(\\d+):.*$")
internal val goldValues = arrayOf<Pair<String, Int>?>(
        null,
        null,
        null,
        null,
        "stack_trace_inline.kt" to 5,
        "stack_trace_inline.kt" to 10)
internal fun checkFrame(value:String) {
    val (pos, file, line) = regex.find(value)!!.destructured
    goldValues[pos.toInt()]?.let{
        assertEquals(it.first, file)
        assertEquals(it.second, line.toInt())
    }
}