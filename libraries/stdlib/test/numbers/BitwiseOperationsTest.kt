/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.numbers

import kotlin.test.*

// TODO: Run these tests during compiler test only (JVM & JS)
class BitwiseOperationsTest {
    @Test fun orForInt() {
        assertEquals(3, 2 or 1)
    }

    @Test fun andForInt() {
        assertEquals(0, 1 and 0)
    }

    @Test fun xorForInt() {
        assertEquals(1, 2 xor 3)
    }

    @Test fun shlForInt() {
        assertEquals(4, 1 shl 2)
    }

    @Test fun shrForInt() {
        assertEquals(1, 2 shr 1)
    }

    @Test fun ushrForInt() {
        assertEquals(2147483647, -1 ushr 1)
    }

    @Test fun invForInt() {
        assertEquals(0, (-1).inv())
    }
}