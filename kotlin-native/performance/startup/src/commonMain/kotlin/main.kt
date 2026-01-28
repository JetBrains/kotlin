/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import org.jetbrains.startup.*

@State(Scope.Benchmark)
// These benchmarks should not be repeated within a single process.
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.NANOSECONDS)
class Singleton {
    @Benchmark
    fun initialize() {
        singletonInitialize()
    }

    @Benchmark
    fun initializeNested() {
        singletonInitializeNested()
    }
}
