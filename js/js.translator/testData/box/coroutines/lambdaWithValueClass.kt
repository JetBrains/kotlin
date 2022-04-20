// EXPECTED_REACHABLE_NODES: 1282
// WITH_STDLIB
package foo

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

value class Res(val value: String)

suspend fun bar(): Res {
    val lambda: suspend () -> Res = { Res("OK") }
    return lambda.invoke()
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
        result = bar().toString().substring(10, 12)
    }

    return result
}
