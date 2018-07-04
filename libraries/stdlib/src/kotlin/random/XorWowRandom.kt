/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

/**
 * Random number generator, algorithm "xorwow" from p. 5 of Marsaglia, "Xorshift RNGs"
 */
internal class XorWowRandom internal constructor(
    private var s0: Int,
    private var s1: Int,
    private var s2: Int,
    private var s3: Int,
    private var addent: Int
) : Random() {

    public constructor(seed: Long) : this(seed.toInt(), (seed ushr 32).toInt(), 0, if (seed == 0L) 1 else 0, 1)
    public constructor(seed: Int) : this(seed, 0, 0, if (seed == 0) 1 else 0, 1)

    init {
        require((s0 or s1 or s2 or s3) != 0) { "Initial state must have at least one non-zero element" }
    }

    override fun nextInt(): Int {
        var t = s3
        t = t xor (t shr 2)
        t = t xor (t shl 1)
        s3 = s2
        s2 = s1
        val s = s0
        s1 = s
        t = t xor s
        t = t xor (s shl 4)
        s0 = t
        addent += 362437
        return t + addent
    }

    override fun nextBits(bitCount: Int): Int = nextInt() ushr (32 - bitCount)
}
