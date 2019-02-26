/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package unsigned

import kotlin.test.Test
import kotlin.test.expect

class UComparisonsTest {

    @Test
    fun minOf_2() {
        expect(1.toUByte()) { minOf(2.toUByte(), 1.toUByte()) }
        expect(58.toUShort()) { minOf(58.toUShort(), 32768.toUShort()) }
        expect(UInt.MIN_VALUE) { minOf(UInt.MIN_VALUE, UInt.MAX_VALUE) }
        expect(42312uL) { minOf(42312uL, 42312uL) }
    }

    @Test
    fun minOf_3() {
        expect(1.toUByte()) { minOf(2.toUByte(), 1.toUByte(), 3.toUByte()) }
        expect(55.toUShort()) { minOf(58.toUShort(), 32768.toUShort(), 55.toUShort()) }
        expect(UInt.MIN_VALUE) { minOf(UInt.MIN_VALUE, UInt.MAX_VALUE, 0u) }
        expect(42312uL) { minOf(42312uL, 42312uL, 42312uL) }
    }

    @Test
    fun maxOf_2() {
        expect(2.toUByte()) { maxOf(2.toUByte(), 1.toUByte()) }
        expect(32768.toUShort()) { maxOf(58.toUShort(), 32768.toUShort()) }
        expect(UInt.MAX_VALUE) { maxOf(UInt.MIN_VALUE, UInt.MAX_VALUE) }
        expect(42312uL) { maxOf(42312uL, 42312uL) }
    }

    @Test
    fun maxOf_3() {
        expect(3.toUByte()) { maxOf(2.toUByte(), 1.toUByte(), 3.toUByte()) }
        expect(32768.toUShort()) { maxOf(58.toUShort(), 32768.toUShort(), 55.toUShort()) }
        expect(UInt.MAX_VALUE) { maxOf(UInt.MIN_VALUE, UInt.MAX_VALUE, 0u) }
        expect(42312uL) { maxOf(42312uL, 42312uL, 42312uL) }
    }
}