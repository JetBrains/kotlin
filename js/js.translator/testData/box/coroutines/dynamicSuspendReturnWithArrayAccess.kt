// WITH_STDLIB

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun foo(): dynamic {
    return js("[1, 2, 3]")
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {}
    })
}

fun box(): String {
    var result: dynamic = 0
    val count = 0;

    builder {
        result += foo()[count + 0] + foo()[count + 1] * foo()[count + 2]
    }

    return if (result == 7) "OK" else "fail: the wrong answer. Expect to have 7 but got $result"
}
