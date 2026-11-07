/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.concurrent

import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

internal object ThreadPool {
    private var _workers: Array<Worker>? = null

    public fun init(threadCount: Int) {
        _workers = Array(threadCount) { Worker.start() }
    }

    public fun deinit() {
        _workers?.forEach {
            it.requestTermination().result
        }
        _workers = null
    }

    public fun <T> execute(f: (Int) -> T): List<Future<T>> = _workers?.mapIndexed { index, worker ->
        worker.execute(TransferMode.SAFE, { Pair(f, index) }) { [f, index] ->
            f(index)
        }
    } ?: error("Call ThreadPool.init() first")
}
