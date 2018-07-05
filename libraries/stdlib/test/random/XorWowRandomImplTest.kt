/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.random

import kotlin.math.pow
import kotlin.random.*
import kotlin.test.*

class XorWowRandomImplTest {
    @Test
    fun predefinedSequence() {
        val seed = 1
        val addend = (seed shl 10) xor (seed ushr 4)

        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        val random: Random = XorWowRandom(seed, 0, 0, 0, 0, addend)

        // differs from reference 0.8178000247146859 because of different double mixing algorithm
        assertEquals(0.817799582443095, random.nextDouble())

        assertEquals(0.8407576507888734, random.nextBits(31) / (2.0.pow(31)))
        assertEquals(533150816, random.nextInt())
    }
}