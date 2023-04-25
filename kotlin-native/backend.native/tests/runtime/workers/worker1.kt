/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(ObsoleteWorkersApi::class)
package runtime.workers.worker1

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest() {
    val COUNT = 5
    val workers = Array(COUNT, { _ -> Worker.start()})

    for (attempt in 1 .. 3) {
        val futures = Array(workers.size,
                { i -> workers[i].execute(TransferMode.SAFE, { "$attempt: Input $i" })
                { input -> input + " processed" }
        })
        futures.forEachIndexed { index, future ->
            future.consume {
                result ->
                if ("$attempt: Input $index processed" != result) {
                    println("Got unexpected $result")
                    throw Error(result)
                }
            }
        }
    }
    workers.forEach {
        it.requestTermination().result
    }
    println("OK")
}
