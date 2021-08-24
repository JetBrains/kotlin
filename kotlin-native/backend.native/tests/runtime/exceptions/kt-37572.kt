import kotlin.native.internal.*
import kotlin.text.Regex
import kotlin.test.*

fun main() {
    val frame = runtimeGetCurrentFrame()
    try {
        foo()
    } catch (tw:Throwable) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
        val stackTrace = tw.getStackTrace()
        stackTrace.take(6).forEach(::checkFrame)
    }
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
internal val goldValues = arrayOf<Pair<String, Int>?>(
        null,
        null,
        "kt-37572.kt" to 28,
        "kt-37572.kt" to 19,
        "kt-37572.kt" to 8,
        "kt-37572.kt" to 5)

internal fun checkFrame(value:String) {
    val (pos, file, line) = regex.find(value)!!.destructured
    goldValues[pos.toInt()]?.let{
        assertEquals(it.first, file)
        assertEquals(it.second, line.toInt())
    }
}