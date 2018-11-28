// EXPECTED_REACHABLE_NODES: 1305
// IGNORE_BACKEND: JS_IR

import kotlin.coroutines.*

suspend inline fun doTwice(block: suspend () -> Unit) {
    block()
    block()
}

var testResult: String = ""

fun build(c: suspend () -> Unit) {
    c.startCoroutine(Continuation<Unit>(EmptyCoroutineContext) { })
}

fun box(): String {

    build {
        doTwice {
            testResult +=  "OK"
            return@build
        }
    }

    return testResult
}