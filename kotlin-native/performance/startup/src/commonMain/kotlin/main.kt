/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.benchmark.*
import org.jetbrains.startup.*

@State(Scope.Benchmark)
// These benchmarks should not be repeated within a single process.
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.NANOSECONDS)
class Singleton {
    @Benchmark
    fun initialize(bh: Blackhole) {
        singletonInitialize(bh)
    }

    @Benchmark
    fun initializeNested(bh: Blackhole) {
        singletonInitializeNested(bh)
    }
}
