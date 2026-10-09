/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.ring

import kotlinx.benchmark.*
import kotlin.math.round

private const val BENCHMARK_SIZE = 10_000

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class FloatingPointRoundingBenchmark {
    private val doubleValues = DoubleArray(BENCHMARK_SIZE) { index ->
        val sign = if ((index % 2) == 0) 0L else Long.MIN_VALUE
        // Visit every normal Double exponent once per 2046 indices.
        val exponent = (1 + index * 73 % 2046).toLong() shl 52
        // An odd multiplier gives distinct 52-bit fractions for the first 2^52 indices.
        val fraction = (index.toLong() * 0x9E37_79B9_7F4A_7C15uL.toLong()) and 0x000f_ffff_ffff_ffffL
        Double.fromBits(sign or exponent or fraction)
    }

    private val floatValues = FloatArray(BENCHMARK_SIZE) { index ->
        val sign = if ((index % 2) == 0) 0 else Int.MIN_VALUE
        // generate an exponent in 1..254, hitting each possible value once per each 254 indices
        val exponent = (1 + index * 73 % 254) shl 23
        // An odd multiplier gives distinct 23-bit fractions for the first 2^23 indices.
        val fraction = (index * 0x45d9f3b) and 0x7fffff
        Float.fromBits(sign or exponent or fraction)
    }

    @Benchmark
    fun double(blackhole: Blackhole) {
        var accumulator = 0L

        for (value in doubleValues) {
            val rounded = round(value)
            accumulator += rounded.toRawBits()
        }

        blackhole.consume(accumulator)
    }

    @Benchmark
    fun float(blackhole: Blackhole) {
        var accumulator = 0

        for (value in floatValues) {
            val rounded = round(value)
            accumulator += rounded.toRawBits()
        }

        blackhole.consume(accumulator)
    }
}
