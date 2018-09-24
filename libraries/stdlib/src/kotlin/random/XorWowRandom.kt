/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

/**
 * Random number generator, algorithm "xorwow" from p. 5 of Marsaglia, "Xorshift RNGs".
 *
 * Cycles after 2^160 * (2^32-1) repetitions.
 *
 * See http://www.jstatsoft.org/v08/i14/paper for details.
 */
internal class XorWowRandom
internal constructor(
    private var x: Int,
    private var y: Int,
    private var z: Int,
    private var w: Int,
    private var v: Int,
    private var addend: Int
) : Random() {

    internal constructor(seed1: Int, seed2: Int) :
            this(seed1, seed2, 0, 0, seed1.inv(), (seed1 shl 10) xor (seed2 ushr 4))

    init {
        require((x or y or z or w or v) != 0) { "Initial state must have at least one non-zero element." }

        // some trivial seeds can produce several values with zeroes in upper bits, so we discard first 64
        repeat(64) { nextInt() }
    }

    override fun nextInt(): Int {
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
