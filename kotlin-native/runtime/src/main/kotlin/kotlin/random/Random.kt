/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.random

import kotlin.native.internal.GCUnsafeCall

internal object NativeRandom : Random() {
    override fun nextBits(bitCount: Int): Int {
        val allBits = nextRandomULong()
        return (allBits shr (48 - bitCount)).toInt()
    }

    /**
     * Overrides the LCG generator state on the current thread with the given seed.
     *
     * The only intended use is testing.
     */
    fun overrideSeed(seed: Long): Unit = overrideSeed(seed.toULong())
}

internal actual fun defaultPlatformRandom(): Random = NativeRandom

internal actual fun doubleFromParts(hi26: Int, low27: Int): Double =
        (hi26.toLong().shl(27) + low27) / (1L shl 53).toDouble()

@GCUnsafeCall("Kotlin_random_nextRandomULongTL")
private external fun nextRandomULong(): ULong

@GCUnsafeCall("Kotlin_random_overrideSeedTL")
private external fun overrideSeed(seed: ULong): Unit
