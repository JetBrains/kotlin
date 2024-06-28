// EXPECTED_REACHABLE_NODES: 1299
// ES_MODULES
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
        if (e !== exception) return "Fail"
    }

    return "OK"
}