/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.ring

import kotlinx.benchmark.*
import kotlin.concurrent.Volatile
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.Worker
import kotlin.random.Random

private const val BENCHMARK_SIZE = 1_000_000

@OptIn(ObsoleteWorkersApi::class)
@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class DefaultRandomBenchmark {
    @Param("1", "2", "4")
    var threads: Int = 0

    val random: Random = Random
    @Volatile
    var runWorkers = true
    lateinit var workers: Array<Worker>

    @Setup
    fun setUp() {
        require(threads > 0) { "Threads must be > 0" }
        workers = Array(threads - 1) { Worker.start() }

        for (worker in workers) {
            worker.executeAfter(0) {
                while (this.runWorkers) {
                    val _ = this.random.nextInt()
                }
            }
        }
    }

    @TearDown
    fun tearDown() {
        runWorkers = false
        workers.forEach { it.requestTermination().result }
    }

    @Benchmark
    fun benchmark(blackhole: Blackhole) {
        var accumulator = 0
        repeat(BENCHMARK_SIZE) {
            accumulator = accumulator xor random.nextInt()
        }

        blackhole.consume(accumulator)
    }
}
