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
    fun behavesAsXorWow() {
        for (seed in listOf(0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 0x123456789abcdefL)) {
            val reference = XorWowRandom(seed1 = seed.toInt(), seed2 = (seed shr 32).toInt())
            NativeRandom.overrideSeed(seed)
            repeat(100) {
                for (bitCount in 0..32) {
                    assertEquals(reference.nextBits(bitCount), NativeRandom.nextBits(bitCount),
                            "Seed $seed, bitCount $bitCount")
                }
            }
        }
    }

    @Test
    fun stateIsThreadLocal() {
        val workers = Array(2) { Worker.start() }
        try {
            // Seed both workers before drawing, so shared state cannot pass this test.
            workers.forEachIndexed { index, worker ->
                worker.execute(TransferMode.SAFE, { index.toLong() }) { seed ->
                    NativeRandom.overrideSeed(seed)
                }.result
            }
            repeat(2) { round ->
                workers.forEachIndexed { index, worker ->
                    val actual = worker.execute(TransferMode.SAFE, { }) {
                        List(100) { NativeRandom.nextInt() }
                    }.result
                    val seed = index.toLong()
                    val reference = XorWowRandom(seed1 = seed.toInt(), seed2 = (seed shr 32).toInt())
                    repeat(round * 100) { val _ = reference.nextInt() }
                    assertEquals(List(100) { reference.nextInt() }, actual)
                }
            }
        } finally {
            workers.forEach { it.requestTermination().result }
        }
    }
}
