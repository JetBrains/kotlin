/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.ring

import kotlinx.benchmark.*
import org.jetbrains.kotlin.benchmarks.collections.BaseIterableSnapshotBenchmark
import org.jetbrains.kotlin.benchmarks.collections.BaseSingletonListSnapshotBenchmark

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
@Measurement(time = 500, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
open class IterableSnapshotBenchmark : BaseIterableSnapshotBenchmark() {
    @Benchmark
    fun toTypedArray(blackhole: Blackhole) {
        blackhole.consume(collection.toTypedArray())
    }
}

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
@Measurement(time = 500, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
open class SingletonListSnapshotBenchmark : BaseSingletonListSnapshotBenchmark() {
    @Benchmark
    fun toTypedArray(blackhole: Blackhole) {
        blackhole.consume(collection.toTypedArray())
    }
}
