// EXPECTED_REACHABLE_NODES: 1296
// IGNORE_BACKEND: JS_IR
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
    assertEquals("fail", ok)
    callback()
    return ok
}