import kotlin.text.Regex
import kotlin.test.*

var inlinesCount = 0

fun main() {
    try {
        foo()
    } catch (tw:Throwable) {
        val stackTrace = tw.getStackTrace();
        inlinesCount = stackTrace.count { it.contains("[inlined]")}
        stackTrace.take(6).forEach(::checkFrame)
    }
    println(inlinesCount)
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
            null,
            null,
            "kt-37572.kt" to 29,
            "kt-37572.kt" to 20,
            *(if (inlinesCount != 0) arrayOf(
                    "kt-37572.kt" to 25,
                    "kt-37572.kt" to 18,
            ) else emptyArray()),
            "kt-37572.kt" to 8,
            "kt-37572.kt" to 6)

    val (pos, file, line) = regex.find(value)!!.destructured
    goldValues[pos.toInt()]?.let {
        assertEquals(it.first, file)
        assertEquals(it.second, line.toInt())
    }
}