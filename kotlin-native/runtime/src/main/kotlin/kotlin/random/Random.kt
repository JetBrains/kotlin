/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(ExperimentalAtomicApi::class)

package kotlin.random

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch
import kotlin.native.concurrent.ThreadLocal
import kotlin.system.getTimeNanos

@ThreadLocal
internal actual val defaultRandom: Random = XorWowRandom(seed1 = SeedSource.nextInt(), seed2 = SeedSource.nextInt())

internal actual fun doubleFromParts(hi26: Int, low27: Int): Double =
        (hi26.toLong().shl(27) + low27) / (1L shl 53).toDouble()

/**
 * Implementation of pseudo-random generator using the linear congruential generator.
 * This only serves to seed instances of [XorWowRandom] that are created per-thread.
 */
private object SeedSource {
    private const val MULTIPLIER = 0x5deece66dL

    @OptIn(ExperimentalAtomicApi::class)
    @Suppress("DEPRECATION_ERROR")
    private val seed = AtomicLong(mult(getTimeNanos()))

    private fun mult(value: Long) = (value xor MULTIPLIER) and ((1L shl 48) - 1)

    private fun nextBits(bitCount: Int): Int {
        val newSeed = seed.updateAndFetch { seed ->
            (seed * MULTIPLIER + 0xbL) and ((1L shl 48) - 1)
        }
        return (newSeed ushr (48 - bitCount)).toInt()
    }

    fun nextInt(): Int = nextBits(32)
}
