/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

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
internal expect class XorWowRandom internal constructor(
    x: Int,
    y: Int,
    z: Int,
    w: Int,
    v: Int,
    addend: Int,
) : Random {
    internal constructor(seed1: Int, seed2: Int)

    override fun nextInt(): Int
    override fun nextBits(bitCount: Int): Int
}
