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
class WithIndicies : SkipWhenBaseOnly() {
    private var _data: ArrayList<Value>? = null
    val data: ArrayList<Value>
        get() = _data!!

    init {
        val list = ArrayList<Value>(BENCHMARK_SIZE)
        for (n in classValues(BENCHMARK_SIZE))
            list.add(n)
        _data = list
    }

    @Benchmark
    fun withIndicies(bh: Blackhole) {
        var result = 0
        for ((index, value) in data.withIndex()) {
            if (filterLoad(value)) {
                result += index + value.value
            }
        }
        bh.consume(result)
    }

    @Benchmark
    fun withIndiciesManual(bh: Blackhole) {
        skipWhenBaseOnly()
        var result = 0
        var index = 0
        for (value in data) {
            if (filterLoad(value)) {
                result += index + value.value
            }
            index++
        }
        bh.consume(result)
    }
}
