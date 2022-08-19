// TARGET_BACKEND: JS_IR
// SPLIT_PER_MODULE
// WITH_STDLIB
// FILE: main.kt
package test.main

import kotlinx.browser.*
import kotlinx.webworkers.annotations.WebWorker
import org.w3c.dom.DedicatedWorkerGlobalScope
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker

fun box(): String {
    return "ok"
}

@WebWorker("demo_worker")
fun worker(self: DedicatedWorkerGlobalScope) {
    self.addEventListener("message", { event ->
        if (event !is MessageEvent) return@addEventListener

        self.postMessage(event.data)
    })

    foo()
}

fun foo() {
    bar()
    var x = 12
    println(x)
}

fun bar() {
    println(window.location)
}