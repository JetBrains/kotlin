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
class IntStream : SkipWhenBaseOnly() {
    private var _data: Iterable<Int>? = null
    val data: Iterable<Int>
        get() = _data!!

    init {
        _data = intValues(BENCHMARK_SIZE)
    }

    @Benchmark
    fun copy(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(data.asSequence().toList())
    }

    @Benchmark
    fun copyManual(bh: Blackhole) {
        skipWhenBaseOnly()
        val list = ArrayList<Int>()
        for (item in data.asSequence()) {
            list.add(item)
        }
        bh.consume(list)
    }

    @Benchmark
    fun filterAndCount(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(data.asSequence().filter { filterLoad(it) }.count())
    }

    @Benchmark
    fun filterAndMap(bh: Blackhole) {
        skipWhenBaseOnly()
        var result = 0
        for (item in data.asSequence().filter { filterLoad(it) }.map { mapLoad(it) })
            result += item.length
        bh.consume(result)
    }

    @Benchmark
    fun filterAndMapManual(bh: Blackhole) {
        skipWhenBaseOnly()
        var result = 0
        for (it in data.asSequence()) {
            if (filterLoad(it)) {
                result += mapLoad(it).length
            }
        }
        bh.consume(result)
    }

    @Benchmark
    fun filter(bh: Blackhole) {
        skipWhenBaseOnly()
        var result = 0
        for (item in data.asSequence().filter { filterLoad(it) })
            result += item
        bh.consume(result)
    }

    @Benchmark
    fun filterManual(bh: Blackhole){
        skipWhenBaseOnly()
        var result = 0
        for (it in data.asSequence()) {
            if (filterLoad(it))
                result += it
        }
        bh.consume(result)
    }

    @Benchmark
    fun countFilteredManual(bh: Blackhole) {
        skipWhenBaseOnly()
        var count = 0
        for (it in data.asSequence()) {
            if (filterLoad(it))
                count++
        }
        bh.consume(count)
    }

    @Benchmark
    fun countFiltered(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(data.asSequence().count { filterLoad(it) })
    }

    @Benchmark
    fun countFilteredLocal(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(data.asSequence().cnt { filterLoad(it) })
    }

    @Benchmark
    fun reduce(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(data.asSequence().fold(0) {acc, it -> if (filterLoad(it)) acc + 1 else acc })
    }
}
