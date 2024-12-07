// NATIVE_STANDALONE
// DISABLE_NATIVE: optimizationMode=NO
// DISABLE_NATIVE: optimizationMode=OPT

import kotlin.text.Regex
import kotlin.test.*

fun exception() {
    error("FAIL!")
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
fun box(): String {
    try {
        exception()
    }
    catch (e:Exception) {
        val stackTrace = e.getStackTrace().filter { "kfun:" in it }.take(6)
        val goldValues = arrayOf(
                "kfun:kotlin.Throwable#<init>(kotlin.String?){}",
                "kfun:kotlin.Exception#<init>(kotlin.String?){}",
                "kfun:kotlin.RuntimeException#<init>(kotlin.String?){}",
                "kfun:kotlin.IllegalStateException#<init>(kotlin.String?){}",
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