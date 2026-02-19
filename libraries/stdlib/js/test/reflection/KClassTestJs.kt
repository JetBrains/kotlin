/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.reflection

import kotlin.test.*

class KClassTestJs {

    @Test
    fun testQualifiedName() {
        @Suppress("UNSUPPORTED", "UNSUPPORTED_REFLECTION_API")
        assertNull(KClassTestJs::class.qualifiedName)
    }

    @Test
    fun testNothing() {
        assertFailsWith<UnsupportedOperationException> { Nothing::class.js }
    }

    private interface I

    @Test
    fun testIsInterface() {
        assertTrue(I::class.isInterface)
        assertFalse(KClassTestJs::class.isInterface)
        assertFalse(Nothing::class.isInterface)
        assertFalse(Int::class.isInterface)
    }
}
