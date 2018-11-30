/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js.worker

import kotlin.js.Promise

class WebWorker<T>(private val worker: dynamic, initialMessage: dynamic) {
    init {
        aliveWorkers.add(WorkerWrapper(worker))
        worker.postMessage(initialMessage)
    }

    fun waitForReply(): Promise<T> {
        return Promise { resolve, reject -> worker.on("message") { e -> resolve(e) }}
    }

    companion object {
        private val aliveWorkers = arrayListOf<WorkerWrapper>()
        fun terminateWorkers() {
            for (worker in aliveWorkers) {
                worker.worker.terminate()
            }
        }
    }
}

// TODO: this shall be internal and exported from module
fun terminateWorkers() {
    WebWorker.terminateWorkers()
}

private class WorkerWrapper(val worker: dynamic)


fun <T> worker(c: () -> T): WebWorker<T> {
    throw UnsupportedOperationException("Implemented as intrinsic")
}

fun postMessage(message: dynamic) {
    js("wt.parentPort.postMessage(message)")
}
