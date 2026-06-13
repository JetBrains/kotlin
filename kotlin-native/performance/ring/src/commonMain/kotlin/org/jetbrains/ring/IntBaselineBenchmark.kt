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

package org.jetbrains.ring

import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

private const val BENCHMARK_SIZE = 10000

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class IntBaseline : SkipWhenBaseOnly() {
    @Benchmark
    fun consume(bh: Blackhole) {
        skipWhenBaseOnly()
        for (item in 1..BENCHMARK_SIZE) {
            // TODO: what does this benchmark measure? `consume` may be too expensive
            bh.consume(item)
        }
    }

    @Benchmark
    fun allocateList(bh: Blackhole) {
        skipWhenBaseOnly()
        val list = ArrayList<Int>(BENCHMARK_SIZE)
        bh.consume(list)
    }

    @Benchmark
    fun allocateArray(bh: Blackhole) {
        skipWhenBaseOnly()
        val list = IntArray(BENCHMARK_SIZE)
        bh.consume(list)
    }

    @Benchmark
    fun allocateListAndFill(bh: Blackhole) {
        skipWhenBaseOnly()
        val list = ArrayList<Int>(BENCHMARK_SIZE)
        for (item in 1..BENCHMARK_SIZE) {
            list.add(item)
        }
        bh.consume(list)
    }

    @Benchmark
    fun allocateArrayAndFill(bh: Blackhole) {
        skipWhenBaseOnly()
        var index = 0
        val list = IntArray(BENCHMARK_SIZE)
        for (item in 1..BENCHMARK_SIZE) {
            list[index++] = item
        }
        bh.consume(list)
    }
}
