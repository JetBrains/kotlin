/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly
import org.jetbrains.structsProducedByMacrosBenchmarks.*
import org.jetbrains.structsBenchmarks.*
import org.jetbrains.typesBenchmarks.*

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class StringBenchmarkHideName {
    private val instance = StringBenchmark()

    @Benchmark
    fun stringToC(bh: Blackhole) {
        instance.stringToCBenchmark(bh)
    }

    @Benchmark
    fun stringToKotlin(bh: Blackhole) {
        instance.stringToKotlinBenchmark(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class IntMatrixBenchmarkHideName {
    private val instance = IntMatrixBenchmark()

    @Benchmark
    fun intMatrix(bh: Blackhole) {
        instance.intMatrixBenchmark(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class CinteropHideName : SkipWhenBaseOnly() {
    @Benchmark
    fun macros(bh: Blackhole) {
        skipWhenBaseOnly()
        macrosBenchmark(bh)
    }

    @Benchmark
    fun struct(bh: Blackhole) {
        skipWhenBaseOnly()
        structBenchmark(bh)
    }

    @Benchmark
    fun union(bh: Blackhole) {
        skipWhenBaseOnly()
        unionBenchmark(bh)
    }

    @Benchmark
    fun enum(bh: Blackhole) {
        skipWhenBaseOnly()
        enumBenchmark(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class IntBenchmarkHideName : SkipWhenBaseOnly() {
    private val instance = IntBenchmark()

    @Benchmark
    fun int(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.intBenchmark(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class BoxedIntBenchmarkHideName : SkipWhenBaseOnly() {
    private val instance = BoxedIntBenchmark()

    @Benchmark
    fun boxedInt(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.boxedIntBenchmark(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class PinnedArrayBenchmarkHideName : SkipWhenBaseOnly() {
    private val instance = PinnedArrayBenchmark()

    @Benchmark
    fun pinnedArray(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.pinnedArrayBenchmark(bh)
    }
}
