// WITH_STDLIB

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun foo(): dynamic {
    return js("{ test: 'OK' }")
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {}
    })
}

fun box(): String {
    var result = ""

    builder {
        result += foo().test
    }

    return result
}
