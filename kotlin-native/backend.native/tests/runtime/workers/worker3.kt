/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(ObsoleteWorkersApi::class)
package runtime.workers.worker3

import kotlin.test.*

import kotlin.native.concurrent.*

data class DataParam(var int: Int)
data class WorkerArgument(val intParam: Int, val dataParam: DataParam)
data class WorkerResult(val intResult: Int, val stringResult: String)

@Test fun runTest() {
    main(emptyArray())
}

fun main(args: Array<String>) {
    val worker = Worker.start()
    val dataParam = DataParam(17)
    val future = try {
        worker.execute(TransferMode.SAFE,
                { WorkerArgument(42, dataParam) }) {
            input -> WorkerResult(input.intParam, input.dataParam.toString() + " result")
        }
    } catch (e: IllegalStateException) {
        null
    }
    if (future != null && Platform.memoryModel == MemoryModel.STRICT)
        println("Fail 1")
    if (dataParam.int != 17) println("Fail 2")
    worker.requestTermination().result
    println("OK")
}
