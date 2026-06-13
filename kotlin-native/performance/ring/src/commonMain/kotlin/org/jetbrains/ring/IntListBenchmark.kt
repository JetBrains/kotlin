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
class IntList : SkipWhenBaseOnly() {
    private var _data: List<Int>? = null
    val data: List<Int>
        get() = _data!!

    init {
        val list = ArrayList<Int>(BENCHMARK_SIZE)
        for (n in intValues(BENCHMARK_SIZE))
            list.add(n)
        _data = list
    }

    @Benchmark
    fun copy(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(data.toList())
    }

    @Benchmark
    fun copyManual(bh: Blackhole) {
        skipWhenBaseOnly()
        val list = ArrayList<Int>(data.size)
        for (item in data) {
            list.add(item)
        }
        bh.consume(list)
    }

    @Benchmark
    fun filterAndCount(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(data.filter { filterLoad(it) }.count())
    }

    @Benchmark
    fun filterAndMap(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(data.filter { filterLoad(it) }.map { mapLoad(it) })
    }

    @Benchmark
    fun filterAndMapManual(bh: Blackhole) {
        skipWhenBaseOnly()
        val list = ArrayList<String>()
        for (it in data) {
            if (filterLoad(it)) {
                val value = mapLoad(it)
                list.add(value)
            }
        }
        bh.consume(list)
    }

    @Benchmark
    fun filter(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(data.filter { filterLoad(it) })
    }

    @Benchmark
    fun filterManual(bh: Blackhole) {
        skipWhenBaseOnly()
        val list = ArrayList<Int>()
        for (it in data) {
            if (filterLoad(it))
                list.add(it)
        }
        bh.consume(list)
    }

    @Benchmark
    fun countFilteredManual(bh: Blackhole) {
        skipWhenBaseOnly()
        var count = 0
        for (it in data) {
            if (filterLoad(it))
                count++
        }
        bh.consume(count)
    }

    @Benchmark
    fun countFiltered(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(data.count { filterLoad(it) })
    }

    @Benchmark
    fun countFilteredLocal(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(data.cnt { filterLoad(it) })
    }

    @Benchmark
    fun reduce(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(data.fold(0) { acc, it -> if (filterLoad(it)) acc + 1 else acc })
    }
}
