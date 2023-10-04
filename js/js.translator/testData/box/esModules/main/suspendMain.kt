// EXPECTED_REACHABLE_NODES: 1296
// ES_MODULES
// CALL_MAIN

import kotlin.coroutines.*

var ok: String = "fail"

var callback: () -> Unit = {}

suspend fun main(args: Array<String>) {
    if (0 != args.size) error("Fail")

    suspendCoroutine<Unit> { cont ->
        callback = {
            cont.resume(Unit)
        }
    }

    ok = "OK"
}

fun box(): String {
    if ("fail" != ok) return "Fail"
    callback()
    return ok
}