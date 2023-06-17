@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

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
        val stackTrace = e.getStackTrace().filter { "kfun:" in it }
        println("Kotlin part of call stack is:")
        for (entry in stackTrace)
            println(entry)
        println("Verifying...")
        val goldValues = arrayOf(
                "kfun:kotlin.Throwable#<init>(kotlin.String?){}",
                "kfun:kotlin.Exception#<init>(kotlin.String?){}",
                "kfun:kotlin.RuntimeException#<init>(kotlin.String?){}",
                "kfun:kotlin.IllegalStateException#<init>(kotlin.String?){}",
                "kfun:#exception(){}",
                "kfun:#main(){}",
        )
        assertEquals(goldValues.size, stackTrace.size)
        goldValues.zip(stackTrace).forEach { checkFrame(it.first, it.second) }
        println("Passed")
    }
}

internal val regex = Regex("(kfun.+) \\+ (\\d+)")
internal fun checkFrame(goldFunName: String, actualLine: String) {
    val findResult = regex.find(actualLine)

    val (funName, offset) = findResult?.destructured ?: throw Error("Cannot find '$goldFunName + <int>' in $actualLine")
    assertEquals(goldFunName, funName)
    assertTrue(offset.toInt() > 0)
}