// EXPECTED_REACHABLE_NODES: 1299
// IGNORE_BACKEND: JS_IR
// CALL_MAIN

import kotlin.coroutines.*

var callback: () -> Unit = {}

val exception = Exception()

suspend fun main() {

    suspendCoroutine<Unit> { cont ->
        callback = {
            cont.resume(Unit)
        }
    }

    throw exception
}

fun box(): String {
    try {
        callback()
    } catch (e: Exception) {
        assertTrue(e === exception)
    }

    return "OK"
}