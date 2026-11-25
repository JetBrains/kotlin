// ISSUE: KT-91731
// A closure created in a loop of a suspend function must capture the loop-local variables of the iteration in which it was created,
// also when the function is suspended in that iteration

// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

var postponed: (() -> Unit)? = null

suspend fun suspendHere(): Unit = suspendCoroutine { continuation ->
    postponed = { continuation.resume(Unit) }
}

suspend fun plainLoop(): List<() -> Int> {
    val acc = mutableListOf<() -> Int>()
    for (n in 3 downTo 1) {
        suspendHere()
        acc.add({ n })
    }
    return acc
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "Fail: not completed"
    builder {
        val actual = plainLoop().map { it() }
        result = if (actual == listOf(3, 2, 1)) "OK" else "Fail: $actual"
    }
    while (true) {
        val resume = postponed ?: break
        postponed = null
        resume()
    }
    return result
}
