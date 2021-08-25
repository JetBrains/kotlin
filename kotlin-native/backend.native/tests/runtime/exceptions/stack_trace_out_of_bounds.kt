import kotlin.text.Regex
import kotlin.test.*

fun main() {
    try {
        val array = intArrayOf(1, 2, 3, 4)
        println(array[4])
    }
    catch (e:Exception) {
        val stackTrace = e.getStackTrace()
        stackTrace.take(goldValues.size).forEach(::checkFrame)
    }
}


internal val regex = Regex("^(\\d+)\\ +.*/(.*):(\\d+):.*$")
internal val goldValues = arrayOf(
        "Throwable.kt" to null,
        "Exceptions.kt" to null,
        "Exceptions.kt" to null,
        "Exceptions.kt" to null,
        "Exceptions.kt" to null,
        "RuntimeUtils.kt" to null,
        "Arrays.cpp" to null,
        "stack_trace_out_of_bounds.kt" to 7,
        "stack_trace_out_of_bounds.kt" to 4,
        "launcher.cpp" to null,
)
internal fun checkFrame(value:String) {
    val (pos, file, line) = regex.find(value)!!.destructured
    goldValues[pos.toInt()]?.let{
        assertEquals(it.first, file)
        if (it.second != null) {
            assertEquals(it.second, line.toInt())
        }
    }
}