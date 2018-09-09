// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1250
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

private var next: () -> Unit = {}
private var stopped = false

suspend fun delay(): Unit = suspendCoroutine { c ->
    next = { c.resume(Unit) }
}

fun build(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override fun resumeWith(x: Result<Unit>) {
            stopped = true
        }

        override val context = EmptyCoroutineContext
    })
}

fun box(): String {
    var log = ""

    build {
        try {
            log += "before delay;"
            delay()
            log += "after delay;"
            js("undefined").lalala
            log += "ignore;"
        }
        catch (e: dynamic) {
            log += "caught ${e.name};"
        }
        finally {
            log += "finally;"
        }
    }

    while (!stopped) {
        log += "@;"
        next()
    }

    if (log != "before delay;@;after delay;caught TypeError;finally;") return "fail: $log"

    return "OK"
}