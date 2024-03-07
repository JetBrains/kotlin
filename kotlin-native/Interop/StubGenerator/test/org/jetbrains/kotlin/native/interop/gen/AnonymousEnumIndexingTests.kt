/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnonymousEnumIndexingTests : IndexerTestsBase() {
    @Test
    fun `named enum`() {
        val enum = indexEnum("""
            enum E { ZERO };
        """.trimIndent())
        assertFalse(enum.isAnonymous)
        assertEquals("enum E", enum.spelling)
    }

    @Test
    fun `typedef enum`() {
        val enum = indexEnum("""
            typedef enum { ZERO } E;
        """.trimIndent())
        assertFalse(enum.isAnonymous)
        assertEquals("E", enum.spelling)
    }

    @Test
    fun `unnamed enum`() {
        val enum = indexEnum("""
            enum { ZERO };
        """.trimIndent())
        assertTrue(enum.isAnonymous)
    }

    @Test
    fun `unnamed enum global`() {
        val enum = indexEnum("""
            enum { ZERO } global;
        """.trimIndent())
        assertTrue(enum.isAnonymous)
    }
}
