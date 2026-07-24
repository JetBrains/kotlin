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

package org.jetbrains.typesBenchmarks

import kotlin.random.Random
import kotlinx.benchmark.*
import kotlinx.cinterop.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

private const val BENCHMARK_SIZE = 1000

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class StringBenchmarkHideName {
    // Use the same seed for reproducibility
    private val rnd = Random(756)

    val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    val randomString = generateRandomString()
    val randomChar = charPool[rnd.nextInt(0, charPool.size)]
    val strings = mutableListOf<String>()

    init {
        // Generate random strings.
        for (i in 1..BENCHMARK_SIZE) {
            strings.add(generateRandomString())
        }
    }

    fun generateRandomString(): String {
        return (1..BENCHMARK_SIZE)
                .map { i -> rnd.nextInt(0, charPool.size) }
                .map(charPool::get)
                .joinToString("")
    }

    @Benchmark
    fun stringToC(bh: Blackhole) {
        var result = 0
        for (i in 1..BENCHMARK_SIZE) {
            result += charFrequency(randomString, randomChar.code.toByte())
        }
        bh.consume(result)
    }

    @Benchmark
    fun stringToKotlin(bh: Blackhole) {
        memScoped {
            for (i in 1..BENCHMARK_SIZE) {
                val pointer = findSuitableString(strings.toCStringArray(this), BENCHMARK_SIZE, "a")
                bh.consume(pointer?.toKString())
                freeSuitableString(pointer)
            }
        }
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class IntMatrixBenchmarkHideName {
    val matrixSize = 1000
    val first = generateMatrix(matrixSize)
    val second = generateMatrix(matrixSize)

    fun generateMatrix(size: Int): Array<IntArray> {
        val matrix = Array(size, { IntArray(size) })
        for (i in (0 until size)) {
            for (j in (0 until size)) {
                matrix[i][j] = (1..20).random()
            }
        }
        return matrix
    }

    @Benchmark
    fun intMatrix(bh: Blackhole) {
        memScoped {
            val result = allocArray<CPointerVar<IntVar>>(matrixSize)
            for (i in (0 until matrixSize)) {
                result[i] = allocArray<IntVar>(matrixSize)
            }
            val resultMatrix = multiplyMatrix(matrixSize, matrixSize,
                    first.map { it.toCValues().ptr }.toCValues().ptr,
                    matrixSize, matrixSize,
                    second.map { it.toCValues().ptr }.toCValues().ptr)
            val resultOutput = buildString {
                for (i in (0 until matrixSize)) {
                    for (j in (0 until matrixSize)) {
                        append(resultMatrix!![i]!![j])
                        append(" ")
                    }
                    append("\n")
                }
            }
            bh.consume(resultOutput)
        }
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class IntBenchmarkHideName : SkipWhenBaseOnly() {
    val size = 20
    val array = Array<Int>(size, { (0 until size).random() })

    @Benchmark
    fun int(bh: Blackhole) {
        skipWhenBaseOnly()
        var result = 0.0
        for (i in 1..BENCHMARK_SIZE) {
            result += average(array[0], array[1], array[2], array[3], array[4], array[5], array[6], array[7], array[8],
                    array[9], array[10], array[11], array[12], array[13], array[14], array[15], array[16],
                    array[17], array[18], array[19])
        }
        bh.consume(result)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class BoxedIntBenchmarkHideName : SkipWhenBaseOnly() {
    val size = 20
    val array = Array<Int?>(size, { null })

    init {
        for (i in (0 until size)) {
            val element: Int? = (0 until size).random()
            array[i] = element
        }
    }

    @Benchmark
    fun boxedInt(bh: Blackhole) {
        skipWhenBaseOnly()
        var result = 0.0
        for (i in 1..BENCHMARK_SIZE) {
            result += average(array[0]!!, array[1]!!, array[2]!!, array[3]!!, array[4]!!, array[5]!!, array[6]!!, array[7]!!, array[8]!!,
                    array[9]!!, array[10]!!, array[11]!!, array[12]!!, array[13]!!, array[14]!!, array[15]!!, array[16]!!,
                    array[17]!!, array[18]!!, array[19]!!)
        }
        bh.consume(result)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class PinnedArrayBenchmarkHideName : SkipWhenBaseOnly() {
    val size = 36
    val vec1 = FloatArray(size) { it.toFloat() / 10 }
    val vec2 = FloatArray(size) { it.toFloat() / 3 }

    @Benchmark
    fun pinnedArray(bh: Blackhole) {
        skipWhenBaseOnly()
        for (i in 1..BENCHMARK_SIZE) {
            vec1.usePinned { first ->
                vec2.usePinned { second ->
                    vecSumAssign(size, first.addressOf(0), second.addressOf(0))
                }
            }
        }
        bh.consume(vec2)
    }
}
