/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.ring

import kotlin.random.Random
import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

// Benchmark is inspired by multik library.

private class ComplexDouble(public val re: Double, public val im: Double) {
    public operator fun plus(other: ComplexDouble): ComplexDouble = ComplexDouble(re + other.re, im + other.im)

    public operator fun times(other: ComplexDouble): ComplexDouble =
        ComplexDouble(re * other.re - im * other.im, re * other.im + other.re * im)
}

private class ComplexDoubleArray(public val size: Int) {
    private val data: DoubleArray = DoubleArray(size * 2)

    public operator fun get(index: Int): ComplexDouble {
        val i = index shl 1
        return ComplexDouble(data[i], data[i + 1])
    }

    public operator fun set(index: Int, value: ComplexDouble): Unit {
        val i = index shl 1
        data[i] = value.re
        data[i + 1] = value.im
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ComplexArrays : SkipWhenBaseOnly() {
    private val size = 1000
    private val a = ComplexDoubleArray(size)
    private val b = ComplexDoubleArray(size)

    init {
        // Use the same seed for reproducibility
        val rnd = Random(6478)
        for (i in 0 until size) {
            a[i] = ComplexDouble(rnd.nextDouble(), rnd.nextDouble())
            b[i] = ComplexDouble(rnd.nextDouble(), rnd.nextDouble())
        }
    }

    @Benchmark
    fun outerProduct(bh: Blackhole) {
        skipWhenBaseOnly()
        val result = ComplexDoubleArray(size * size)

        for (i in 0 until size) {
            for (j in 0 until size) {
                result[i + j * size] += a[i] * b[j]
            }
        }

        bh.consume(result)
    }
}
