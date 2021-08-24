import kotlin.native.internal.*
import kotlin.text.Regex
import kotlin.test.*

fun exception() {
    error("FAIL!")
}

fun main() {
    val frame = runtimeGetCurrentFrame()
    try {
        exception()
    }
    catch (e:Exception) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
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
        "stack_trace_inline.kt" to 6,
        "stack_trace_inline.kt" to 12)
internal fun checkFrame(value:String) {
    val (pos, file, line) = regex.find(value)!!.destructured
    goldValues[pos.toInt()]?.let{
        assertEquals(it.first, file)
        assertEquals(it.second, line.toInt())
    }
}