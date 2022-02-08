// EXPECTED_REACHABLE_NODES: 1401
// DECLARES_VARIABLE: function=doResume name=k TARGET_BACKENDS=JS
// PROPERTY_READ_COUNT: name=local$o count=1 TARGET_BACKENDS=JS
// PROPERTY_WRITE_COUNT: name=local$o count=2 TARGET_BACKENDS=JS

import kotlin.coroutines.*

var next: () -> Unit = {}
var complete = false
var log = ""

suspend fun foo(x: String): String = suspendCoroutine { continuation ->
    log += "[$x]"
    next = { continuation.resume(x) }
}

fun build(x: suspend () -> Unit) {
    next = {
        x.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(x: Result<Unit>) {
                complete = true
            }
        })
    }
}

fun box(): String {
    build {
        val o = foo("O")
        log += "-"
        val k = foo("K")
        log += ":"
        log += "{$o$k}"
    }

    while (!complete) {
        next()
        log += "#"
    }

    if (log != "[O]#-[K]#:{OK}#") return "fail: $log"

    return "OK"
}