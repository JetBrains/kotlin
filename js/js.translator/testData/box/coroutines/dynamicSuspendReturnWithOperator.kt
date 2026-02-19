// WITH_STDLIB

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun foo(): dynamic {
    return 2
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {}
    })
}

fun box(): String {
    var result: dynamic = 1

    builder {
        result += foo() + 2 + result + foo()
    }

    return if (result == 8) "OK" else "fail: the wrong answer. Expect to have 8 but got $result"
}
