/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.freeze0

import kotlin.test.*

import kotlin.native.concurrent.*

data class SharedDataMember(val double: Double)

data class SharedData(val string: String, val int: Int, val member: SharedDataMember)

@Test fun runTest() {
    val worker = Worker.start()
    // Create immutable shared data.
    val immutable = SharedData("Hello", 10, SharedDataMember(0.1)).freeze()
    println("frozen bit is ${immutable.isFrozen}")

    val future = worker.execute(TransferMode.SAFE, { immutable } ) {
        input ->
        println("Worker: $input")
        input
    }
    future.consume {
        result -> println("Main: $result")
    }
    worker.requestTermination().result
    println("OK")
}