/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*

class UByteValueSetTest {

    @Test
    fun acceptsEveryByteValueExactlyOnce() {
        val set = UByteValueSet()
        for (value in 0..255) {
            assertTrue(set.add(value.toUByte()), "first add of $value")
        }
        for (value in 0..255) {
            assertFalse(set.add(value.toUByte()), "second add of $value")
        }
    }

    @Test
    fun addsDistinctValuesIndependently() {
        val set = UByteValueSet()

        assertTrue(set.add(1.toUByte()), "first add of 1")
        assertTrue(set.add(70.toUByte()), "first add of 70")
        assertTrue(set.add(150.toUByte()), "first add of 150")
        assertTrue(set.add(220.toUByte()), "first add of 220")

        assertFalse(set.add(1.toUByte()), "second add of 1")
        assertFalse(set.add(150.toUByte()), "second add of 150")

        assertTrue(set.add(2.toUByte()), "first add of 2")
        assertTrue(set.add(151.toUByte()), "first add of 151")
    }
}
