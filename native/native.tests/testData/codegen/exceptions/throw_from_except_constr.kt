// NATIVE_STANDALONE
// FREE_COMPILER_ARGS: -Xbinary=sourceInfoType=libbacktrace
// DISABLE_NATIVE: targetFamily=MINGW
// DISABLE_NATIVE: optimizationMode=NO
// DISABLE_NATIVE: optimizationMode=OPT

import kotlin.text.Regex
import kotlin.test.*

class CustomException(msg: String) : Exception(msg) {
    init {
        constrException()
    }

    fun constrException() {
        throw Exception("Exception during exception's construction")
    }
}

fun exception() {
    throw CustomException("Exceptional message")
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
fun box(): String {
    try {
        exception()
    }
    catch (e:Exception) {
        val stackTrace = e.getStackTrace().filter { "kfun:" in it }.take(3)
        val goldValues = arrayOf(
                "kfun:CustomException#constrException(){}",
                "kfun:#exception(){}",
                "kfun:#box(){}kotlin.String",
        )
        goldValues.zip(stackTrace).forEach { checkFrame(it.first, it.second) }
    }
    return "OK"
}

internal val regex = Regex("(kfun.+) \\+ (\\d+)")
internal fun checkFrame(goldFunName: String, actualLine: String) {
    val findResult = regex.find(actualLine)

    val (funName, offset) = findResult?.destructured ?: throw Error("Cannot find '$goldFunName + <int>' in $actualLine")
    assertEquals(goldFunName, funName)
    assertTrue(offset.toInt() > 0)
}