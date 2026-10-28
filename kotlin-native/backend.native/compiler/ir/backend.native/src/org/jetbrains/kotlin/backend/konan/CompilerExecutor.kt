/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

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

    fun <T> runAll(tasks: List<() -> T>): List<T>
}

object SequentialCompilerExecutor : CompilerExecutor {
    override fun <T> runAll(tasks: List<() -> T>): List<T> = tasks.map { it.invoke() }

    override fun close() = Unit
}

class ParallelCompilerExecutor(threadsCount: Int) : CompilerExecutor {

    init {
        require(threadsCount > 1) {
            "A parallel executor requires more than one thread."
        }
    }

    private val executor = Executors.newFixedThreadPool(threadsCount)

    override fun <T> runAll(tasks: List<() -> T>): List<T> {
        return tasks.map { task -> executor.submit(task) }.map { it.get() }
    }

    override fun close() {
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.DAYS)
    }
}
