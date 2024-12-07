import kotlin.coroutines.*

private fun runCoroutine(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            result.getOrThrow()
        }
    })
}

fun test(): Int {
    val generator = ClosedRangeGenerator(0, 0, 1)
    generator.resetGenerator()
    var s = 0
    runCoroutine {
        while(generator.hasNext()) {
            s += generator.nextValue()
        }
    }
    return s
}
