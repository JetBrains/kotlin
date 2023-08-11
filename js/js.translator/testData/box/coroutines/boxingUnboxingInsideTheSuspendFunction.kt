// WITH_STDLIB
// EXPECTED_REACHABLE_NODES: 1292
// KT-60785

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

value class SomeValue(val a: String) {
    override fun toString() = when (a) {
        "fa" -> "O"
        "il" -> "K"
        else -> ""
    }
}

suspend fun foo() = mapOf(SomeValue("fa") to SomeValue("il"))

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {}
    })
}

fun box(): String {
    var result = ""

    builder {
        for ((k, v) in foo()) {
            result += "$k$v"
        }
    }

    return result
}
