/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(ObsoleteWorkersApi::class)
package runtime.workers.worker2

import kotlin.test.*

import kotlin.native.concurrent.*

data class WorkerArgument(val intParam: Int, val stringParam: String)
data class WorkerResult(val intResult: Int, val stringResult: String)

@Test fun runTest() {
    val COUNT = 5
    val workers = Array(COUNT, { _ -> Worker.start()})

    for (attempt in 1 .. 3) {
        val futures = Array(workers.size, { workerIndex -> workers[workerIndex].execute(TransferMode.SAFE, {
            WorkerArgument(workerIndex, "attempt $attempt") }) { input ->
                var sum = 0
                for (i in 0..input.intParam * 1000) {
                    sum += i
                }
                WorkerResult(sum, input.stringParam + " result")
            }
        })
        val futureSet = futures.toSet()
        var consumed = 0
        while (consumed < futureSet.size) {
            val ready = waitForMultipleFutures(futureSet, 10000)
            ready.forEach {
                it.consume { result ->
                    if (result.stringResult != "attempt $attempt result") throw Error("Unexpected $result")
                    consumed++
                }
            }
        }
    }
    workers.forEach {
        it.requestTermination().result
    }
    println("OK")
}
