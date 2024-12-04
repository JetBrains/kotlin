/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test

import kotlin.metadata.jvm.JvmMetadataVersion
import kotlin.test.*

class JvmMetadataVersionTest {

    fun mv(major: Int, minor: Int, patch: Int) = JvmMetadataVersion(major, minor, patch)

    @Test
    fun testEquality() {
        assertEquals(mv(1, 2, 3), mv(1, 2, 3))
        assertNotEquals(mv(0, 2, 3), mv(1, 2, 3))
        assertNotEquals(mv(1, 0, 3), mv(1, 2, 3))
        assertNotEquals(mv(1, 2, 3), mv(1, 2, 0))
    }

    @Test
    fun testComparison() {
        assertTrue(mv(1, 2, 3) > mv(1, 2, 0))
        assertTrue(mv(1, 0, 3) < mv(1, 2, 0))
        assertTrue(mv(1, 2, 3) < mv(2, 0, 0))
    }

    @Test
    fun testConstructor() {
        assertFailsWith<IllegalArgumentException> { mv(-1, 0, 0) }
        assertFailsWith<IllegalArgumentException> { mv(0, -1, 0) }
        assertFailsWith<IllegalArgumentException> { mv(0, 0, -1) }
    }
}
