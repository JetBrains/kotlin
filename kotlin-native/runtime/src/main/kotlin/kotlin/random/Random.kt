/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.random

import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
internal actual val defaultRandom: Random = NativeRandom()

internal actual fun doubleFromParts(hi26: Int, low27: Int): Double =
        (hi26.toLong().shl(27) + low27) / (1L shl 53).toDouble()

/**
 * The default implementation of pseudo-random generator using the linear congruential generator.
 * Even though it's not thread safe per se, it's exposed via [defaultRandom] annotated with [ThreadLocal],
 * making the [Random.Default] thread safe on Native platforms.
 */
private class NativeRandom : Random() {
    private var state = stateFromSeed(SeedAllocator.nextSeed())
    private fun stateFromSeed(seed: Long) = (seed xor MULTIPLIER) and MASK

    override fun nextBits(bitCount: Int): Int {
        val nextState = (state * MULTIPLIER + INCREMENT) and MASK
        state = nextState
        return (nextState ushr (MODULUS - bitCount)).toInt()
    }

    /** Resets the state using the given seed for testing purposes */
    fun overrideSeed(seed: Long) {
        state = stateFromSeed(seed)
    }

    private val MULTIPLIER = 0x5deece66dL
    private val INCREMENT = 0xbL
    private val MODULUS = 48
    private val MASK = (1L shl MODULUS) - 1
}

/** Resets the state of [NativeRandom] using the given seed for testing purposes */
internal fun overrideNativeRandomSeed(seed: Long) = (defaultRandom as NativeRandom).overrideSeed(seed)