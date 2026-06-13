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

import kotlin.random.Random
import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

private const val BENCHMARK_SIZE = 10000

/**
 * This class emulates matrix behaviour using a hash map as its implementation
 */
class KMatrix internal constructor(val rows: Int, val columns: Int, data: DoubleArray) {
    private val matrix: MutableMap<Pair<Int, Int>, Double> = HashMap()

    init {
        for (i in data.indices) {
            val row = i / columns
            val col = i % columns
            matrix.put(Pair(row, col), data[i])
        }
    }

    fun get(row: Int, col: Int): Double {
        return get(Pair(row, col))
    }

    fun get(pair: Pair<Int, Int>): Double {
        return matrix.getOrElse(pair, { 0.0 })
    }

    fun put(pair: Pair<Int, Int>, elem: Double) {
        matrix.put(pair, elem)
    }

    operator fun plusAssign(other: KMatrix) {
        for (entry in matrix.entries) {
            put(entry.key, entry.value + other.get(entry.key))
        }
    }
}

/**
 * This class tests hash map performance
 */
@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class MatrixMap {
    // Use the same seed for reproducibility
    private val rnd = Random(501)

    private val data1 = DoubleArray(BENCHMARK_SIZE) {
        rnd.nextDouble()
    }

    private val data2 = DoubleArray(BENCHMARK_SIZE) {
        rnd.nextDouble()
    }

    private var rows = BENCHMARK_SIZE
    private var cols = 1
    init {
        while (rows > cols) {
            rows /= 2
            cols *= 2
        }
    }

    @Benchmark
    fun add(bh: Blackhole) {
        val a = KMatrix(rows, cols, data1)
        val b = KMatrix(rows, cols, data2)
        a += b
        bh.consume(a)
    }

}
