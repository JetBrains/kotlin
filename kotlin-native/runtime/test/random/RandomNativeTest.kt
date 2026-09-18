/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.random

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*
import kotlin.random.*
import kotlin.test.*

// Native-specific part of stdlib/test/random/RandomTest.kt
class SeededRandomSmokeNativeTest {
    val subject: Random get() = seededRandomSmokeTestSubject

    @Test
    fun sameIntSeedNextLong() {
        val v = subject.nextInt(1..Int.MAX_VALUE)
        for (seed in listOf(v, -v)) {
            testSameSeededRandoms(Random(seed), Random(seed), seed) { nextLong() }
        }
    }

    @Test
    fun sameIntSeedNextIntWithLimit() {
        val v = subject.nextInt(1..Int.MAX_VALUE)
        for (seed in listOf(v, -v)) {
            testSameSeededRandoms(Random(seed), Random(seed), seed) { nextInt(1000) }
        }
    }

    private inline fun <reified T> testSameSeededRandoms(r1: Random, r2: Random, seed: Any, generator: Random.() -> T) {
        val seq1 = List(10) { r1.generator() }
        val seq2 = List(10) { r2.generator() }

        assertEquals(seq1, seq2, "Generators seeded with $seed should produce the same output")
    }
}

class MultiThreadedRandomSmokeTest {
    val subject: Random get() = Random

    @Test
    fun nextInt() {
        val workers = Array(10) { Worker.start() }
        val canStart = AtomicInt(0)
        val futures = workers.map {
            it.execute(TransferMode.SAFE, { subject to canStart }) { [subject, canStart] ->
                var result1 = 0
                var result2 = -1
                @Suppress("ControlFlowWithEmptyBody")
                while (canStart.value == 0) {}
                repeat(100) {
                    val r = subject.nextInt()
                    result1 = result1 or r
                    result2 = result2 and r
                }
                result1 to result2
            }
        }
        canStart.value = 1
        var result1 = 0
        var result2 = -1
        futures.forEach {
            val [r1, r2] = it.result
            result1 = result1 or r1
            result2 = result2 and r2
        }
        assertEquals(-1, result1, "All one bits should present")
        assertEquals(0, result2, "All zero bits should present")
        workers.forEach {
            it.requestTermination().result
        }
    }
}

class NativeRandomTest {

    @Test
    fun behavesAsLCG() {
        val seed = Random.nextLong()
        val reference = LCGRandom(seed)
        NativeRandom.overrideSeed(seed)
        val seq1 = List(10) { reference.nextInt() }
        val seq2 = List(10) { NativeRandom.nextInt() }

        assertEquals(seq1, seq2, "The both generators produce the same output")
    }

    private class LCGRandom(seed: Long) : Random() {
        private var state = mult(seed)

        private fun mult(value: Long) = (value xor MULTIPLIER) and MASK

        override fun nextBits(bitCount: Int): Int {
            val nextState = (state * MULTIPLIER + INCREMENT) and MASK
            state = nextState
            return (nextState ushr (MODULUS - bitCount)).toInt()
        }

        companion object {
            private const val MULTIPLIER = 0x5deece66dL
            private const val INCREMENT = 0xbL
            private const val MODULUS = 48
            private const val MASK = (1L shl MODULUS) - 1
        }
    }
}
