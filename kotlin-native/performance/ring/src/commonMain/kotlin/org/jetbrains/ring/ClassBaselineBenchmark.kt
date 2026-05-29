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
class ClassBaseline : SkipWhenBaseOnly() {

    @Benchmark
    fun consume(bh: Blackhole) {
        for (item in 1..BENCHMARK_SIZE) {
            // TODO: what does this benchmark measure? `consume` may be too expensive
            bh.consume(Value(item))
        }
    }

    @Benchmark
    fun consumeField(bh: Blackhole) {
        val value = Value(0)
        for (item in 1..BENCHMARK_SIZE) {
            value.value = item
            // TODO: what does this benchmark measure? `consume` may be too expensive
            bh.consume(value)
        }
    }

    @Benchmark
    fun allocateList(bh: Blackhole) {
        skipWhenBaseOnly()
        val list = ArrayList<Value>(BENCHMARK_SIZE)
        bh.consume(list)
    }

    @Benchmark
    fun allocateArray(bh: Blackhole) {
        skipWhenBaseOnly()
        val list = arrayOfNulls<Value>(BENCHMARK_SIZE)
        bh.consume(list)
    }

    @Benchmark
    fun allocateListAndFill(bh: Blackhole) {
        val list = ArrayList<Value>(BENCHMARK_SIZE)
        for (item in 1..BENCHMARK_SIZE) {
            list.add(Value(item))
        }
        bh.consume(list)
    }

    @Benchmark
    fun allocateListAndWrite(bh: Blackhole) {
        skipWhenBaseOnly()
        val value = Value(0)
        val list = ArrayList<Value>(BENCHMARK_SIZE)
        for (item in 1..BENCHMARK_SIZE) {
            list.add(value)
        }
        bh.consume(list)
    }

    @Benchmark
    fun allocateArrayAndFill(bh: Blackhole) {
        skipWhenBaseOnly()
        val list = arrayOfNulls<Value>(BENCHMARK_SIZE)
        var index = 0
        for (item in 1..BENCHMARK_SIZE) {
            list[index++] = Value(item)
        }
        bh.consume(list)
    }
}
