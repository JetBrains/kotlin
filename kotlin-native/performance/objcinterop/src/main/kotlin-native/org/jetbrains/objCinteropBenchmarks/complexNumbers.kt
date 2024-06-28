/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.complexNumbers
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import platform.Foundation.*
import platform.darwin.*

actual typealias ComplexNumber = Complex

actual class ComplexNumbersBenchmark actual constructor() {
    val complexNumbersSequence = generateNumbersSequence()

    fun randomNumber() = Random.nextDouble(0.0, benchmarkSize.toDouble())

    actual fun generateNumbersSequence(): List<Complex> {
        val result = mutableListOf<Complex>()
        for (i in 1..benchmarkSize) {
            result.add(Complex(randomNumber(), randomNumber()))
        }
        return result
    }

    actual fun sumComplex() {
        complexNumbersSequence.map { it.add(it) }.reduce { acc, it -> acc.add(it) }
    }

    actual fun subComplex() {
        complexNumbersSequence.map { it.sub(it) }.reduce { acc, it -> acc.sub(it) }
    }

    actual fun classInheritance() {
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

        val result = InvertedNumber(0.0)

        for (i in 1..benchmarkSize) {
            result.add(InvertedNumber(randomNumber()))
            result.sub(InvertedNumber(randomNumber()))
        }
    }

    actual fun categoryMethods() {
        complexNumbersSequence.map { it.mul(it) }.reduce { acc, it -> acc.mul(it) }
        complexNumbersSequence.map { it.div(it) }.reduce { acc, it -> acc.mul(it) }
    }

    actual fun stringToObjC() {
        complexNumbersSequence.forEach {
            it.setFormat("%.1lf|%.1lf")
        }
    }

    actual fun stringFromObjC() {
        complexNumbersSequence.forEach {
            it.description()?.split(" ")
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

    actual fun fft() {
        fftRoutine()
    }

    actual fun invertFft() {
        val sequence = fftRoutine(true)

        sequence.forEachIndexed { index, number ->
            sequence[index] = number.div(Complex(sequence.size.toDouble(), 0.0))
        }
    }
}
