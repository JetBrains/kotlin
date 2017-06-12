// EXPECTED_REACHABLE_NODES: 552
// FILE: a.kt
// WITH_RUNTIME
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun suspendThere(v: String): String = suspendCoroutineOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

suspend fun suspendHere(): String = suspendThere("O") + suspendThere("K")

// FILE: b.kt
// RECOMPILE
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext

        override fun resume(result: Unit) {}

        override fun resumeWithException(exception: Throwable) {}
    })
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
