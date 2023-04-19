/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(ObsoleteWorkersApi::class)
package runtime.workers.worker7

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest() {
    val worker = Worker.start(false)
    val future = worker.execute(TransferMode.SAFE, { "Input" }) {
        input -> println(input)
    }
    future.consume {
        result -> println("Got $result")
    }

    assertFailsWith<IllegalStateException> {
        println(worker.execute(TransferMode.SAFE, { null }, { _ -> throw Error("An error") }).result)
    }

    worker.requestTermination().result

    println("OK")
}
