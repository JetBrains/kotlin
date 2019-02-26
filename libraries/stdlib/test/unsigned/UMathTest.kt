/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.unsigned

import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.expect

class UMathTest {

    @Test
    fun min() {
        expect(1uL) { min(2uL, 1uL) }
        expect(ULong.MIN_VALUE) { min(ULong.MIN_VALUE, ULong.MAX_VALUE) }
        expect(58u) { min(58u, 1u shl 31) }
        expect(42312u) { min(42312u, 42312u) }
    }

    @Test
    fun max() {
        expect(2u) { max(2u, 1u) }
        expect(UInt.MAX_VALUE) { maxOf(UInt.MIN_VALUE, UInt.MAX_VALUE) }
        expect(1uL shl 63) { max(58uL, 1uL shl 63) }
        expect(42312uL) { maxOf(42312uL, 42312uL) }
    }
}
