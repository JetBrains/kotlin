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

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly
import org.jetbrains.structsProducedByMacrosBenchmarks.*
import org.jetbrains.structsBenchmarks.*
import org.jetbrains.typesBenchmarks.*

@State(Scope.Benchmark)
class StringBenchmarkHideName {
    private val instance = StringBenchmark()

    @Benchmark
    fun stringToC() {
        instance.stringToCBenchmark()
    }

    @Benchmark
    fun stringToKotlin() {
        instance.stringToKotlinBenchmark()
    }
}

@State(Scope.Benchmark)
class IntMatrixBenchmarkHideName {
    private val instance = IntMatrixBenchmark()

    @Benchmark
    fun intMatrix() {
        instance.intMatrixBenchmark()
    }
}

@State(Scope.Benchmark)
class CinteropHideName : SkipWhenBaseOnly() {
    @Benchmark
    fun macros() {
        skipWhenBaseOnly()
        macrosBenchmark()
    }

    @Benchmark
    fun struct() {
        skipWhenBaseOnly()
        structBenchmark()
    }

    @Benchmark
    fun union() {
        skipWhenBaseOnly()
        unionBenchmark()
    }

    @Benchmark
    fun enum() {
        skipWhenBaseOnly()
        enumBenchmark()
    }
}

@State(Scope.Benchmark)
class IntBenchmarkHideName : SkipWhenBaseOnly() {
    private val instance = IntBenchmark()

    @Benchmark
    fun int() {
        skipWhenBaseOnly()
        instance.intBenchmark()
    }
}

@State(Scope.Benchmark)
class BoxedIntBenchmarkHideName : SkipWhenBaseOnly() {
    private val instance = BoxedIntBenchmark()

    @Benchmark
    fun boxedInt() {
        skipWhenBaseOnly()
        instance.boxedIntBenchmark()
    }
}

@State(Scope.Benchmark)
class PinnedArrayBenchmarkHideName : SkipWhenBaseOnly() {
    private val instance = PinnedArrayBenchmark()

    @Benchmark
    fun pinnedArray() {
        skipWhenBaseOnly()
        instance.pinnedArrayBenchmark()
    }
}