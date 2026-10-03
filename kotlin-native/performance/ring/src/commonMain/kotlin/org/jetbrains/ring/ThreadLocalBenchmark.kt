/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.ring

import kotlinx.benchmark.*
import kotlin.native.concurrent.ThreadLocal
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ThreadLocalBenchmark : SkipWhenBaseOnly() {
    @Benchmark
    fun primitiveReadingAndWriting(blackhole: Blackhole) {
        skipWhenBaseOnly()

        repeat(BENCHMARK_SIZE) {
            counter++
        }

        blackhole.consume(counter)
    }

    @Benchmark
    fun objectReading(blackhole: Blackhole) {
        skipWhenBaseOnly()

        repeat(BENCHMARK_SIZE) {
            Counter.increment()
        }

        blackhole.consume(Counter.value)
    }

    @Benchmark
    fun referenceReading(blackhole: Blackhole) {
        skipWhenBaseOnly()

        repeat(BENCHMARK_SIZE) { index ->
            blackhole.consume(reference)
        }
    }

    @Benchmark
    fun referenceReadingAndWriting(blackhole: Blackhole) {
        skipWhenBaseOnly()

        val string = "foobar"

        repeat(BENCHMARK_SIZE) { index ->
            if (index % 2 == 0) {
                reference = string
            } else {
                reference = null
            }
            blackhole.consume(reference)
        }
    }
}

private const val BENCHMARK_SIZE = 10_000

@ThreadLocal
private var counter = 0

@ThreadLocal
private object Counter {
    var value: Int = 0
        private set

    fun increment() = value++
}

@ThreadLocal
private var reference: Any? = null
