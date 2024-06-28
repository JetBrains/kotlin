// NATIVE_STANDALONE
// FREE_COMPILER_ARGS: -Xbinary=sourceInfoType=coresymbolication
// FREE_COMPILER_ARGS: -Xg-generate-debug-trampoline=enable
// DISABLE_NATIVE: isAppleTarget=false
// DISABLE_NATIVE: optimizationMode=NO
// DISABLE_NATIVE: optimizationMode=OPT
// FILE: stack_trace_inline.kt

import kotlin.text.Regex
import kotlin.test.*

val expectedInlinesCount = 0
val expectedExceptionContrFrames = 4

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
            "stack_trace_inline.kt" to 16,
            "stack_trace_inline.kt" to 23)
    val (pos, file, line) = regex.find(value)!!.destructured
    goldValues[pos.toInt()]?.let {
        assertEquals(it.first, file)
        assertEquals(it.second, line.toInt())
    }
}