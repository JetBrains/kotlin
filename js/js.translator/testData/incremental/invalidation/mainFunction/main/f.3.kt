package foo.bar

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