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
}
