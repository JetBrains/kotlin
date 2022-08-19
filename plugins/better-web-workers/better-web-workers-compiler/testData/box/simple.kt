// TARGET_BACKEND: JS_IR
// WITH_STDLIB
// WITH_COROUTINES
// FILE: main.kt
package test.main

import kotlinx.webworkers.annotations.WebWorker
import org.w3c.dom.DedicatedWorkerGlobalScope
import kotlinx.coroutines.*
import kotlin.coroutines.*
import helpers.*
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker
import kotlin.js.Promise

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var result = "fail"
fun box(): String {
    val worker = Worker("demo_worker")

    builder {
        val job = GlobalScope.launch {
            result = Promise { resolve, _ ->
                worker.addEventListener("message", { event ->
                    if (event !is MessageEvent) return@addEventListener

                    val payload = event.data
                    if (payload !is String) return@addEventListener
                    resolve(payload)
                })
            }.await()
        }

        worker.postMessage("OK")
        job.join()
    }

    return result
}

@WebWorker("demo_worker")
fun worker(self: DedicatedWorkerGlobalScope) {
    self.addEventListener("message", { event ->
        if (event !is MessageEvent) return@addEventListener

        self.postMessage(event.data)
    })
}