import kotlin.coroutines.*

fun runCoroutine(coroutine: suspend () -> Int): String {
    var result: String = "Fail: was not run"
    coroutine.startCoroutine(object : Continuation<Int> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(r: Result<Int>) {
            val calculation = r.getOrThrow()
            result = if (calculation == 33) "OK" else "Fail: wrong calculation $calculation"
        }
    })
    return result
}

suspend fun suspendBox(): Int {
    val x = fooX()
    val y = fooY()
    Test.complex()
    return x + y
}

fun box() = runCoroutine { suspendBox() }