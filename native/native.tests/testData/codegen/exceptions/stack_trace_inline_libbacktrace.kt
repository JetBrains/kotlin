// NATIVE_STANDALONE
// FREE_COMPILER_ARGS: -Xbinary=sourceInfoType=libbacktrace
// DISABLE_NATIVE: targetFamily=MINGW
// DISABLE_NATIVE: optimizationMode=NO
// DISABLE_NATIVE: optimizationMode=OPT
// FILE: stack_trace_inline.kt

import kotlin.text.Regex
import kotlin.test.*

val expectedInlinesCount = 1
val expectedExceptionContrFrames = 0

fun exception() {
    error("FAIL!")
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
fun box(): String {
    var actualInlinesCount = 0
    try {
        exception()
    }
    catch (e:Exception) {
        val stackTrace = e.getStackTrace().take(expectedExceptionContrFrames + expectedInlinesCount + 2)
        actualInlinesCount = stackTrace.count { it.contains("[inlined]")}
        stackTrace.forEach(::checkFrame)
    }
    assertEquals(expectedInlinesCount, actualInlinesCount)
    return "OK"
}
internal val regex = Regex("^(\\d+)\\ +.*/(.*):(\\d+):.*$")
internal fun checkFrame(value:String) {
    val goldValues = arrayOf<Pair<String, Int>?>(
            *arrayOfNulls(expectedExceptionContrFrames),
            *arrayOfNulls(expectedInlinesCount),
            "stack_trace_inline.kt" to 15,
            "stack_trace_inline.kt" to 22)
    val (pos, file, line) = regex.find(value)!!.destructured
    goldValues[pos.toInt()]?.let {
        assertEquals(it.first, file)
        assertEquals(it.second, line.toInt())
    }
}