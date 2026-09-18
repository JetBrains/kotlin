/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.random

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch
import kotlin.native.concurrent.ThreadLocal
import kotlin.system.getTimeNanos

@ThreadLocal
internal actual val defaultRandom: Random = NativeRandom()

internal actual fun doubleFromParts(hi26: Int, low27: Int): Double =
        (hi26.toLong().shl(27) + low27) / (1L shl 53).toDouble()

/** Resets the state of [NativeRandom] using the given seed for testing purposes */
internal fun overrideNativeRandomSeed(seed: Long) = (defaultRandom as NativeRandom).overrideSeed(seed)

/**
 * The default implementation of pseudo-random generator using the linear congruential generator.
 * Even though it's not thread safe per se, it's exposed via [defaultRandom] annotated with [ThreadLocal],
 * making the [Random.Default] thread safe on Native platforms.
 */
private class NativeRandom : Random() {
    private var state = stateFromSeed(SeedAllocator.nextSeed())

    override fun nextBits(bitCount: Int): Int {
        val nextState = nextState(state)
        state = nextState
        return nextBitsFromState(nextState, bitCount)
    }

    /** Resets the state using the given seed for testing purposes */
    fun overrideSeed(seed: Long) {
        state = stateFromSeed(seed)
    }

    companion object {
        private const val MULTIPLIER = 0x5deece66dL
        private const val INCREMENT = 0xbL
        private const val MODULUS = 48
        private const val MASK = (1L shl MODULUS) - 1

        fun stateFromSeed(seed: Long) = (seed xor MULTIPLIER) and MASK
        fun nextState(state: Long) = (state * MULTIPLIER + INCREMENT) and MASK
        fun nextBitsFromState(state: Long, bitCount: Int) = (state ushr (MODULUS - bitCount)).toInt()
    }
}

/**
 * Linear congruential generator, just like [NativeRandom], but synchronized using an atomic operation, used as the source of initial seeds
 * for thread local [NativeRandom] instances.
 */
@OptIn(ExperimentalAtomicApi::class)
private object SeedAllocator {
    @Suppress("DEPRECATION_ERROR")
    private val state = AtomicLong(NativeRandom.stateFromSeed(getTimeNanos()))

    fun nextSeed(): Long = nextBits(32).toLong().shl(32) + nextBits(32)

    private fun nextBits(bitCount: Int): Int {
        val nextState = state.updateAndFetch(NativeRandom::nextState)
        return NativeRandom.nextBitsFromState(nextState, bitCount)
    }
}
