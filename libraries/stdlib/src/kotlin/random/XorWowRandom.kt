/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

import kotlin.internal.wrapAsDeserializationException

/**
 * Random number generator, using Marsaglia's "xorwow" algorithm
 *
 * Cycles after 2^192 - 2^32 repetitions.
 *
 * For more details, see Marsaglia, George (July 2003). "Xorshift RNGs". Journal of Statistical Software. 8 (14). doi:10.18637/jss.v008.i14
 *
 * Available at https://www.jstatsoft.org/v08/i14/paper
 *
 */
internal class XorWowRandom internal constructor(
    private var x: Int,
    private var y: Int,
    private var z: Int,
    private var w: Int,
    private var v: Int,
    private var addend: Int
) : Random(), Serializable {

    internal constructor(seed1: Int, seed2: Int) :
            this(seed1, seed2, 0, 0, seed1.inv(), (seed1 shl 10) xor (seed2 ushr 4))

    init {
        checkInvariants()

        // some trivial seeds can produce several values with zeroes in upper bits, so we discard first 64
        repeat(64) { val _ = nextInt() }
    }

    private fun checkInvariants() {
        require((x or y or z or w or v) != 0) { "Initial state must have at least one non-zero element." }
    }

    private fun readResolve(): Any = also { wrapAsDeserializationException { checkInvariants() } }

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

    private companion object {
        private const val serialVersionUID: Long = 0L
    }
}
