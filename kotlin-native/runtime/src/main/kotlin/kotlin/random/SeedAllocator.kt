/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAtomicApi::class)

package kotlin.random

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch
import kotlin.system.getTimeNanos

/**
 * Linear congruential generator, just like [NativeRandom], but synchronized using an atomic operation, used as the source of initial seeds
 * for thread local [NativeRandom] instances.
 */
internal object SeedAllocator {
    @Suppress("DEPRECATION_ERROR")
    private val state = AtomicLong(stateFromSeed(getTimeNanos()))

    fun nextSeed(): Long = nextBits(32).toLong().shl(32) + nextBits(32)

    private fun nextBits(bitCount: Int): Int {
        val nextState = state.updateAndFetch { state ->
            (state * MULTIPLIER + INCREMENT) and MASK
        }
        return (nextState ushr (MODULUS - bitCount)).toInt()
    }

    private fun stateFromSeed(seed: Long): Long = (seed xor MULTIPLIER) and MASK

    private const val MULTIPLIER = 0x5deece66dL
    private const val INCREMENT = 0xbL
    private const val MODULUS = 48
    private const val MASK = (1L shl MODULUS) - 1
}
