/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAtomicApi::class)

package org.jetbrains.ring

import kotlinx.benchmark.*
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.ThreadLocal
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.random.Random
import kotlin.system.getTimeNanos

@OptIn(ObsoleteWorkersApi::class)
@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class RandomBenchmark {
    @Param("atomic-lcg", "thread-local-xor-wow", "atomic-xor-wow")
    var implementation: String = ""

    @Param("1", "10", "100")
    var threads: Int = 0

    @Param(/*"1000",*/ /*"5000",*/ "10000")
    var numbers: Int = 0

    lateinit var random: () -> Int
    var numbersPerThread: Int = 0
    lateinit var workers: Array<Worker>

    @Setup
    fun setUp() {
        random = when (implementation) {
            "atomic-lcg" -> {
                { LCGRandom.nextInt() }
            }
            "thread-local-xor-wow" -> {
                { xorWowThreadLocalRandom.nextInt() }
            }
            "atomic-xor-wow" -> {
                { xorWowAtomicRandom.nextInt() }
            }
            else -> throw IllegalStateException("Unknown implementation $implementation")
        }

        require(threads > 0) { "Threads must be > 0" }
        require(numbers > 0) { "Numbers must be > 0" }
        require(numbers % threads == 0) { "Numbers must be a multiple of threads" }
        numbersPerThread = numbers / threads
        workers = Array(threads) { Worker.start() }
    }

    @TearDown
    fun tearDown() {
        workers.forEach { it.requestTermination().result }
    }

    @Benchmark
    fun benchmark(blackhole: Blackhole) {
        val futures = workers.map {
            it.execute(TransferMode.SAFE, { Triple(random, numbersPerThread, blackhole) }) { [random, iterations, blackhole] ->
                repeat(iterations) {
                    blackhole.consume(random())
                }
            }
        }

        futures.forEach { it.consume {} }
    }
}

@ThreadLocal
private val xorWowThreadLocalRandom = XorWowRandom(seed1 = LCGRandom.nextInt(), seed2 = LCGRandom.nextInt())
private val xorWowAtomicRandom = XorWowAtomicRandom(seed1 = LCGRandom.nextInt(), seed2 = LCGRandom.nextInt())

/** 1:1 copy (barring serialization stuff) of [kotlin.random.XorWowRandom] */
private class XorWowRandom constructor(
        private var x: Int,
        private var y: Int,
        private var z: Int,
        private var w: Int,
        private var v: Int,
        private var addend: Int
) : Random() {

    constructor(seed1: Int, seed2: Int) :
            this(seed1, seed2, 0, 0, seed1.inv(), (seed1 shl 10) xor (seed2 ushr 4))

    init {
        checkInvariants()

        // some trivial seeds can produce several values with zeroes in upper bits, so we discard first 64
        repeat(64) { val _ = nextInt() }
    }

    private fun checkInvariants() {
        require((x or y or z or w or v) != 0) { "Initial state must have at least one non-zero element." }
    }

    override fun nextInt(): Int {
        // Equivalent to the xorxow algorithm
        // From Marsaglia, G. 2003. Xorshift RNGs. J. Statis. Soft. 8, 14, p. 5
        var t = x
        t = t xor (t ushr 2)
        x = y
        y = z
        z = w
        val v0 = v
        w = v0
        t = (t xor (t shl 1)) xor v0 xor (v0 shl 4)
        v = t
        addend += 362437
        return t + addend
    }

    override fun nextBits(bitCount: Int): Int =
            nextInt().takeUpperBits(bitCount)
}

/** Copy of [kotlin.random.XorWowRandom] made thread-safe with atomics */
private class XorWowAtomicRandom(
        x: Int,
        y: Int,
        z: Int,
        w: Int,
        v: Int,
        addend: Int
) : Random() {

    private val state = AtomicReference(State(x, y, z, w, v, addend))

    constructor(seed1: Int, seed2: Int) :
            this(seed1, seed2, 0, 0, seed1.inv(), (seed1 shl 10) xor (seed2 ushr 4))

    init {
        state.load().checkInvariants()

        // some trivial seeds can produce several values with zeroes in upper bits, so we discard first 64
        repeat(64) { val _ = nextInt() }
    }

    private class State(val x: Int, val y: Int, val z: Int, val w: Int, val v: Int, val addend: Int) {
        fun checkInvariants() {
            require((x or y or z or w or v) != 0) { "Initial state must have at least one non-zero element." }
        }
    }

    override fun nextInt(): Int {
        // Equivalent to the xorxow algorithm
        // From Marsaglia, G. 2003. Xorshift RNGs. J. Statis. Soft. 8, 14, p. 5
        val updatedState = state.updateAndFetch { currentState ->
            var t = currentState.x
            t = t xor (t ushr 2)
            val x = currentState.y
            val y = currentState.z
            val z = currentState.w
            val v0 = currentState.v
            val w = v0
            t = (t xor (t shl 1)) xor v0 xor (v0 shl 4)
            val v = t
            val addend = currentState.addend + 362437
            State(x, y, z, w, v, addend)
        }
        return updatedState.v + updatedState.addend
    }

    override fun nextBits(bitCount: Int): Int =
            nextInt().takeUpperBits(bitCount)
}

private fun Int.takeUpperBits(bitCount: Int): Int =
        this.ushr(32 - bitCount) and (-bitCount).shr(31)

/** Copy of the current [kotlin.random.NativeRandom] + the atomicity fix using [AtomicLong.updateAndFetch] */
private object LCGRandom : Random() {
    private const val MULTIPLIER = 0x5deece66dL

    @OptIn(ExperimentalAtomicApi::class)
    @Suppress("DEPRECATION_ERROR")
    private val seed = AtomicLong(mult(getTimeNanos()))

    private fun mult(value: Long) = (value xor MULTIPLIER) and ((1L shl 48) - 1)

    override fun nextBits(bitCount: Int): Int {
        val newSeed = seed.updateAndFetch { seed ->
            (seed * MULTIPLIER + 0xbL) and ((1L shl 48) - 1)
        }
        return (newSeed ushr (48 - bitCount)).toInt()
    }

    override fun nextInt(): Int = nextBits(32)
}