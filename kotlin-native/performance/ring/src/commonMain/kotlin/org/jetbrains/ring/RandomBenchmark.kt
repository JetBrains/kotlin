/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.ring

import kotlinx.benchmark.*
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.random.Random
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

@OptIn(ObsoleteWorkersApi::class)
@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class RandomBenchmark : SkipWhenBaseOnly() {
    @Param("1", "2", "4", "6", "8", "64")
    var threads: Int = 0

    val numbers: Int = 1_000_000
    val random: Random = Random
    var numbersPerThread: Int = 0
    lateinit var workers: Array<Worker>

    @Setup
    fun setUp() {
        require(threads > 0) { "Threads must be > 0" }
        require(numbers > 0) { "Numbers must be > 0" }
        numbersPerThread = numbers / threads
        workers = Array(threads) { Worker.start() }
    }

    @TearDown
    fun tearDown() {
        workers.forEach { it.requestTermination().result }
    }

    @Benchmark
    fun benchmark(blackhole: Blackhole) {
        skipWhenBaseOnly()

        val futures = workers.map {
            it.execute(TransferMode.SAFE, { Pair(random, numbersPerThread) }) { [random, numbers] ->
                var accumulator = 0
                repeat(numbers) {
                    accumulator = accumulator xor random.nextInt()
                }
                accumulator
            }
        }

        futures.forEach {
            it.consume { result -> blackhole.consume(result) }
        }
    }
}
