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
class Loop : SkipWhenBaseOnly() {
    var arrayList: List<Value>
    var array: Array<Value>

    init {
        val list = ArrayList<Value>(BENCHMARK_SIZE)
        for (n in classValues(BENCHMARK_SIZE))
            list.add(n)
        arrayList = list
        array = list.toTypedArray()
    }

    @Benchmark
    fun arrayLoop(bh: Blackhole) {
        var result = 0
        for (x in array) {
            result += x.value
        }
        bh.consume(result)
    }

    @Benchmark
    fun arrayIndexLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var result = 0
        for (i in array.indices) {
            result += array[i].value
        }
        bh.consume(result)
    }

    @Benchmark
    fun rangeLoop(bh: Blackhole) {
        var result = 0
        for (i in 0..<array.size) {
            result += array[i].value
        }
        bh.consume(result)
    }

    @Benchmark
    fun arrayListLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var result = 0
        for (x in arrayList) {
            result += x.value
        }
        bh.consume(result)
    }

    @Benchmark
    fun arrayWhileLoop(bh: Blackhole) {
        var result = 0
        var i = 0
        val s = array.size
        while (i < s) {
            result += array[i].value
            i++
        }
        bh.consume(result)
    }

    @Benchmark
    fun arrayForeachLoop(bh: Blackhole) {
        var result = 0
        array.forEach { result += it.value }
        bh.consume(result)
    }

    @Benchmark
    fun arrayListForeachLoop(bh: Blackhole) {
        var result = 0
        arrayList.forEach { result += it.value }
        bh.consume(result)
    }
}
