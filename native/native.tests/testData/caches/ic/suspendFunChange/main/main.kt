import kotlin.coroutines.*
import kotlin.test.*
import test.*

fun runCoroutine(coroutine: suspend () -> Int): Int {
    var result = -1
    coroutine.startCoroutine(object : Continuation<Int> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(resultValue: Result<Int>) {
            result = resultValue.getOrThrow()
        }
    })
    return result
}

@Test
fun runTest() {
    assertEquals(1, runCoroutine { bar() })
    assertEquals(2, runCoroutine { baz() })
}
