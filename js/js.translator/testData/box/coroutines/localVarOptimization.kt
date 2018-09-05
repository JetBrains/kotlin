// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1251
// DECLARES_VARIABLE: function=doResume name=k
// PROPERTY_READ_COUNT: name=local$o count=1
// PROPERTY_WRITE_COUNT: name=local$o count=2
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

            override fun resumeWith(x: SuccessOrFailure<Unit>) {
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