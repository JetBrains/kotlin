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

package org.jetbrains.ring.stringBenchmark

import kotlin.random.Random
import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly
import org.jetbrains.ring.*

private const val BENCHMARK_SIZE = 10000

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class String : SkipWhenBaseOnly() {
    private var _data: ArrayList<kotlin.String>? = null
    val data: ArrayList<kotlin.String>
        get() = _data!!
    var csv: kotlin.String = ""

    init {
        // Use the same seed for reproducibility
        val rnd = Random(863)
        val list = ArrayList<kotlin.String>(BENCHMARK_SIZE)
        for (n in stringValues(BENCHMARK_SIZE))
            list.add(n)
        _data = list
        csv = ""
        for (i in 1..BENCHMARK_SIZE-1) {
            val elem = rnd.nextDouble()
            csv += elem
            csv += ","
        }
        csv += 0.0
    }

    @Benchmark
    fun stringConcat(bh: Blackhole) {
        var string: kotlin.String = ""
        for (it in data) string += it
        bh.consume(string)
    }

    @Benchmark
    fun stringConcatNullable(bh: Blackhole) {
        skipWhenBaseOnly()
        var string: kotlin.String? = ""
        for (it in data) string += it
        bh.consume(string)
    }

    @Benchmark
    fun stringBuilderConcat(bh: Blackhole) {
        var string : StringBuilder = StringBuilder("")
        for (it in data) string.append(it)
        bh.consume(string)
    }

    @Benchmark
    fun stringBuilderConcatNullable(bh: Blackhole) {
        var string : StringBuilder? = StringBuilder("")
        for (it in data) string?.append(it)
        bh.consume(string.toString())
    }

    @Benchmark
    fun summarizeSplittedCsv(bh: Blackhole) {
        val fields = csv.split(",")
        var sum = 0.0
        for (field in fields) {
            sum += field.toDouble()
        }
        bh.consume(sum)
    }
}
