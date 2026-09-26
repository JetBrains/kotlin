/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

class NativeDefaultRandomTest {
    val subject: Random get() = Random

    @Test
    fun behavesAsLCG() {
        for (seed in listOf(0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 0x123456789abcdefL)) {
            val reference = Reference(seed)
            overrideNativeRandomSeed(seed)
            repeat(100) {
                for (bitCount in 0..32) {
                    assertEquals(reference.nextBits(bitCount), subject.nextBits(bitCount),
                            "Seed $seed, bitCount $bitCount")
                }
            }
        }
    }

    @Test
    fun stateIsThreadLocal() {
        val workers = Array(2) { Worker.start() }
        try {
            // seed from both workers before drawing, so shared state cannot pass this test
            workers.forEachIndexed { index, worker ->
                worker.execute(TransferMode.SAFE, { index.toLong() }) { seed ->
                    overrideNativeRandomSeed(seed)
                }.result
            }

            val rounds = 2
            val roundSize = 100

            repeat(rounds) { round ->
                workers.forEachIndexed { index, worker ->
                    val actual = worker.execute(TransferMode.SAFE, { Pair(subject, roundSize) }) { [random, size] ->
                        List(size) { random.nextInt() }
                    }.result
                    val seed = index.toLong()
                    val reference = Reference(seed)
                    // bring the reference into the same state that subject is supposed to be in after the previous rounds
                    repeat(round * roundSize) { val _ = reference.nextInt() }

                    assertEquals(List(roundSize) { reference.nextInt() }, actual, "Round: $round, worker: $index")
                }
            }
        } finally {
            workers.forEach { it.requestTermination().result }
        }
    }

    private class Reference(seed: Long) : Random() {
        private var state = (seed xor MULTIPLIER) and MASK

        override fun nextBits(bitCount: Int): Int {
            val nextState = (state * MULTIPLIER + INCREMENT) and MASK
            state = nextState
            return (nextState ushr (MODULUS - bitCount)).toInt()
        }

        override fun nextInt(): Int = nextBits(32)

        companion object {
            const val MULTIPLIER = 0x5deece66dL
            const val INCREMENT = 0xbL
            const val MODULUS = 48
            const val MASK = (1L shl MODULUS) - 1
        }
    }
}
