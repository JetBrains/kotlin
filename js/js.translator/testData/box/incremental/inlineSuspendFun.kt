// EXPECTED_REACHABLE_NODES: 1295
// SKIP_IR_INCREMENTAL_CHECKS
// WITH_STDLIB
// FILE: a.kt
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun bar() = "OK"

inline suspend fun foo() = bar()

// FILE: b.kt
// RECOMPILE
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun baz() = foo()

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext

        override fun resumeWith(result: Result<Unit>) {}
    })
}

fun box(): String {
    var result = ""

    builder {
        result = baz()
    }

    return result
}
