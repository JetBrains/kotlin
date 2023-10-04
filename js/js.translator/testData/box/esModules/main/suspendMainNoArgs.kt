// EXPECTED_REACHABLE_NODES: 1296
// ES_MODULES
// CALL_MAIN

import kotlin.coroutines.*

var ok: String = "fail"

var callback: () -> Unit = {}

suspend fun main() {
    suspendCoroutine<Unit> { cont ->
        callback = {
            cont.resume(Unit)
        }
    }

    ok = "OK"
}

fun box(): String {
    if ("fail" != ok) error("Fail")
    callback()
    return ok
}