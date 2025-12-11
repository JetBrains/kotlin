/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.comparisons

import kotlin.test.*

class BooleanOrderingTest {
    @Test
    fun compareTo() {
        assertEquals(0, true.compareTo(true))
        assertEquals(0, false.compareTo(false))
        assertTrue(true.compareTo(false) > 0)
        assertTrue(false.compareTo(true) < 0)
    }
}
