/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

interface CompilerExecutor : AutoCloseable {
    companion object {
        fun from(threadsCount: Int): CompilerExecutor =
                if (threadsCount > 1)
                    ParallelCompilerExecutor(threadsCount)
                else
                    SequentialCompilerExecutor
    }

    val availableWorkers: Int

    fun <T> runAll(tasks: List<() -> T>): List<T>
}

object SequentialCompilerExecutor : CompilerExecutor {
    override val availableWorkers: Int = 1

    override fun <T> runAll(tasks: List<() -> T>): List<T> = tasks.map { it.invoke() }

    override fun close() = Unit
}

class ParallelCompilerExecutor(threadsCount: Int) : CompilerExecutor {

    init {
        require(threadsCount > 1) {
            "A parallel executor requires more than one thread."
        }
    }

    override val availableWorkers: Int = threadsCount

    @Volatile
    private var failureOccurred: Boolean = false

    private val executor = Executors.newFixedThreadPool(threadsCount)

    override fun <T> runAll(tasks: List<() -> T>): List<T> {
        val futures = executor.invokeAll(tasks.map { Callable { it() } })
        return futures.map { future ->
            try {
                future.get()
            } catch (e: ExecutionException) {
                failureOccurred = true
                throw e.cause ?: e
            }
        }
    }

    override fun close() {
        if (failureOccurred) {
            executor.shutdownNow()
        } else {
            executor.shutdown()
        }
        executor.awaitTermination(1, TimeUnit.DAYS)
    }
}
