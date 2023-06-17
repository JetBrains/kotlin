@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.text.Regex
import kotlin.test.*

var expectedInlinesCount = 0
var expectedExceptionContrFrames = 0

fun main(args: Array<String>) {
    val sourceInfoType = args.first()
    val (e, i) = when (sourceInfoType) {
        "libbacktrace" -> Pair(0, 2)
        "coresymbolication" -> Pair(2, 0)
        else -> throw AssertionError("Unknown source info type " + sourceInfoType)
    }
    expectedExceptionContrFrames = e
    expectedInlinesCount = i

    var actualInlinesCount = 0
    try {
        foo()
    } catch (tw:Throwable) {
        val stackTrace = tw.getStackTrace();
        actualInlinesCount = stackTrace.count { it.contains("[inlined]")}
        stackTrace.take(expectedExceptionContrFrames + 4).forEach(::checkFrame)
    }
    assertEquals(expectedInlinesCount, actualInlinesCount)
}

fun foo() {
    myRun {
        //platform.darwin.NSObject()
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
            "kt-37572.kt" to 42,
            "kt-37572.kt" to 33,
            *(if (expectedInlinesCount != 0) arrayOf(
                    "kt-37572.kt" to 38,
                    "kt-37572.kt" to 31,
            ) else emptyArray()),
            "kt-37572.kt" to 21,
            "kt-37572.kt" to 9)

    val (pos, file, line) = regex.find(value)!!.destructured
    goldValues[pos.toInt()]?.let {
        assertEquals(it.first, file)
        assertEquals(it.second, line.toInt())
    }
}