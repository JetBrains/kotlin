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
class DoubleUlpBenchmark {
    private val normalValues = DoubleArray(BENCHMARK_SIZE) { index ->
        val sign = if ((index % 2) == 0) 0L else Long.MIN_VALUE
        // Visit every normal Double exponent once per 2046 indices.
        val exponent = (1 + index * 73 % 2046).toLong() shl 52
        // An odd multiplier gives distinct 52-bit fractions for the first 2^52 indices.
        val fraction = (index.toLong() * 0x9E37_79B9_7F4A_7C15uL.toLong()) and 0x000f_ffff_ffff_ffffL
        Double.fromBits(sign or exponent or fraction)
    }

    private val specialValues = doubleArrayOf(
            0.0, -0.0, Double.MIN_VALUE, -Double.MIN_VALUE,
            Double.MAX_VALUE, -Double.MAX_VALUE, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.NaN
    )

    private val mixedValues = DoubleArray(BENCHMARK_SIZE) { index ->
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

    private fun consumeUlps(values: DoubleArray, bh: Blackhole) {
        var sum = 0L
        for (value in values) {
            sum += value.ulp.toRawBits()
        }
        bh.consume(sum)
    }
}
