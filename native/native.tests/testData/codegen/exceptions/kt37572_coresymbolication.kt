// NATIVE_STANDALONE
// FREE_COMPILER_ARGS: -Xbinary=sourceInfoType=coresymbolication
// DISABLE_NATIVE: isAppleTarget=false
// DISABLE_NATIVE: optimizationMode=NO
// DISABLE_NATIVE: optimizationMode=OPT
// FILE: kt37572.kt

import kotlin.text.Regex
import kotlin.test.*

val expectedInlinesCount = 0
val expectedExceptionContrFrames = 2

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
fun box(): String {
    var actualInlinesCount = 0
    try {
        foo()
    } catch (tw:Throwable) {
        val stackTrace = tw.getStackTrace().take(expectedExceptionContrFrames + expectedInlinesCount + 3)
        actualInlinesCount = stackTrace.count { it.contains("[inlined]")}
        stackTrace.forEach(::checkFrame)
    }
    assertEquals(expectedInlinesCount, actualInlinesCount)
    return "OK"
}

fun foo() {
    myRun {
        throwException()
    }
}

inline fun myRun(block: () -> Unit) {
    block()
}

fun throwException() {
    throw Error()
}
internal val regex = Regex("^(\\d+)\\ +.*/(.*):(\\d+):.*$")

internal fun checkFrame(value:String) {
    val goldValues = arrayOf<Pair<String, Int>?>(
            *arrayOfNulls(expectedExceptionContrFrames),
            "kt37572.kt" to 39,
            "kt37572.kt" to 30,
            *(if (expectedInlinesCount != 0) arrayOf(
                    "kt37572.kt" to 35,
                    "kt37572.kt" to 29,
            ) else emptyArray()),
            "kt37572.kt" to 18)

    val (pos, file, line) = regex.find(value)!!.destructured
    goldValues[pos.toInt()]?.let {
        assertEquals(it.first, file)
        assertEquals(it.second, line.toInt())
    }
}