/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.complexNumbers

import kotlinx.benchmark.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly
import platform.Foundation.*
import platform.darwin.*

private const val BENCHMARK_SIZE = 1000

typealias ComplexNumber = Complex

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ComplexNumbersBenchmarkHideName : SkipWhenBaseOnly() {
    // Use the same seed for reproducibility
    private val rnd = Random(94)

    val complexNumbersSequence = generateNumbersSequenceImpl()

    fun randomNumber() = rnd.nextDouble(0.0, BENCHMARK_SIZE.toDouble())

    private fun generateNumbersSequenceImpl(): List<Complex> {
        val result = mutableListOf<Complex>()
        for (i in 1..BENCHMARK_SIZE) {
            result.add(Complex(randomNumber(), randomNumber()))
        }
        return result
    }

    private val randomNumbers = DoubleArray(BENCHMARK_SIZE * 2) {
        randomNumber()
    }

    @Benchmark
    fun generateNumbersSequence(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(generateNumbersSequenceImpl())
    }

    @Benchmark
    fun sumComplex(bh: Blackhole) {
        bh.consume(complexNumbersSequence.map { it.add(it) }.reduce { acc, it -> acc.add(it) })
    }

    @Benchmark
    fun subComplex(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(complexNumbersSequence.map { it.sub(it) }.reduce { acc, it -> acc.sub(it) })
    }

    @Benchmark
    fun classInheritance(bh: Blackhole) {
        skipWhenBaseOnly()
         class InvertedNumber(val value: Double) : CustomNumberProtocol, NSObject() {
            override fun add(other: CustomNumberProtocol) : CustomNumberProtocol =
                    if (other is InvertedNumber)
                        InvertedNumber(-value + sqrt(other.value))
                    else
                        error("Expected object of InvertedNumber class")


            override fun sub(other: CustomNumberProtocol) : CustomNumberProtocol =
                    if (other is InvertedNumber)
                        InvertedNumber(-value - sqrt(other.value))
                    else
                        error("Expected object of InvertedNumber class")
        }

        var result = InvertedNumber(0.0)

        var doAdd = true
        for (number in randomNumbers) {
            val inverted = InvertedNumber(number)
            if (doAdd) {
                result = result.add(inverted) as InvertedNumber
            } else {
                result = result.sub(inverted) as InvertedNumber
            }
            doAdd = !doAdd
        }

        bh.consume(result)
    }

    @Benchmark
    fun categoryMethods(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(complexNumbersSequence.map { it.mul(it) }.reduce { acc, it -> acc.mul(it) })
        bh.consume(complexNumbersSequence.map { it.div(it) }.reduce { acc, it -> acc.mul(it) })
    }

    @Benchmark
    fun stringToObjC(bh: Blackhole) {
        complexNumbersSequence.forEach {
            it.setFormat("%.1lf|%.1lf")
            bh.consume(it)
        }
    }

    @Benchmark
    fun stringFromObjC(bh: Blackhole) {
        complexNumbersSequence.forEach {
            bh.consume(it.description()?.split(" "))
        }
    }

    private fun revert(number: Int, lg: Int): Int {
        var result = 0
        for (i in 0 until lg) {
            if (number and (1 shl i) != 0) {
                result = result or 1 shl (lg - i - 1)
            }
        }
        return result
    }

    inline private fun fftRoutine(invert:Boolean = false): Array<Complex> {
        var lg = 0
        while ((1 shl lg) < complexNumbersSequence.size) {
            lg++
        }
        val sequence = complexNumbersSequence.toTypedArray()

        sequence.forEachIndexed { index, number ->
            if (index < revert(index, lg) && revert(index, lg) < complexNumbersSequence.size) {
                sequence[index] = sequence[revert(index, lg)].also { sequence[revert(index, lg)] = sequence[index] }
            }
        }

        var length = 2
        while (length < complexNumbersSequence.size) {
            val angle = 2 * PI / length * if (invert) -1 else 1
            val base = Complex(cos(angle), sin(angle))
            for (i in 0 until complexNumbersSequence.size / 2 step length) {
                var value = Complex(1.0, 1.0)
                for (j in 0 until length/2) {
                    val first = sequence[i + j]
                    val second = sequence[i + j + length/2].mul(value)
                    sequence[i + j] = first.add(second) as Complex
                    sequence[i + j + length/2] = first.sub(second) as Complex
                    value = value.mul(base)
                }
            }
            length = length shl 1
        }
        return sequence
    }

    @Benchmark
    fun fft(bh: Blackhole) {
        bh.consume(fftRoutine())
    }

    @Benchmark
    fun invertFft(bh: Blackhole) {
        skipWhenBaseOnly()
        val sequence = fftRoutine(true)

        sequence.forEachIndexed { index, number ->
            sequence[index] = number.div(Complex(sequence.size.toDouble(), 0.0))
        }
        bh.consume(sequence)
    }
}
