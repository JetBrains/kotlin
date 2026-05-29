/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.ring

import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

private const val BENCHMARK_SIZE = 1000

var counter = 0

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class AllocationBenchmark : SkipWhenBaseOnly() {

    class MyClass {
        fun inc() {
            counter++
        }
    }

    @Benchmark
    fun allocateObjects(bh: Blackhole) {
        skipWhenBaseOnly()
        repeat(BENCHMARK_SIZE) {
            MyClass().inc()
        }
        bh.consume(counter)
    }

}
