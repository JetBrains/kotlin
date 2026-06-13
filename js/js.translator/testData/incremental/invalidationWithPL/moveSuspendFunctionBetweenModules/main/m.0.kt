import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

fun runCoroutine(stepId: Int, coroutine: suspend () -> Int): String {
    var result = "Fail: was not run"
    coroutine.startCoroutine(object : Continuation<Int> {
        override val context = EmptyCoroutineContext

        override fun resumeWith(r: Result<Int>) {
            val expected = when (stepId) {
                0 -> 0
                1 -> 1
                else -> {
                    result = "Unknown"
                    return
                }
            }
            val actual = r.getOrThrow()
            result = if (actual == expected) "OK" else "Fail; got $actual"
        }
    })
    return result
}

fun box(stepId: Int, isWasm: Boolean): String = runCoroutine(stepId) { test() }
