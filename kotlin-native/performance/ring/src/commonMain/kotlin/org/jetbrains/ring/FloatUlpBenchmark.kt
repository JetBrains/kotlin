/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.ring

import kotlinx.benchmark.*
import kotlin.math.ulp

private const val BENCHMARK_SIZE = 10_000

@State(Scope.Benchmark)
@Measurement(time = 500, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class FloatUlpBenchmark {
    private val normalValues = FloatArray(BENCHMARK_SIZE) { index ->
        val sign = if ((index % 2) == 0) 0 else Int.MIN_VALUE
        // generate an exponent in 1..254, hitting each possible value once per each 254 indices
        val exponent = (1 + index * 73 % 254) shl 23
        // An odd multiplier gives distinct 23-bit fractions for the first 2^23 indices.
        val fraction = (index * 0x45d9f3b) and 0x7fffff
        Float.fromBits(sign or exponent or fraction)
    }

    private val specialValues = floatArrayOf(
            0.0f, -0.0f, Float.MIN_VALUE, -Float.MIN_VALUE,
            Float.MAX_VALUE, -Float.MAX_VALUE, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NaN
    )

    private val mixedValues = FloatArray(BENCHMARK_SIZE) { index ->
        val specialValuePeriod = 8
        if ((index % specialValuePeriod) == 0) {
            specialValues[(index / specialValuePeriod) % specialValues.size]
        } else {
            normalValues[index]
        }
    }

    @Benchmark
    fun normal(bh: Blackhole) = consumeUlps(normalValues, bh)

    @Benchmark
    fun mixed(bh: Blackhole) = consumeUlps(mixedValues, bh)

    private fun consumeUlps(values: FloatArray, bh: Blackhole) {
        var sum = 0L
        for (value in values) {
            sum += value.ulp.toRawBits()
        }
        bh.consume(sum)
    }
}
