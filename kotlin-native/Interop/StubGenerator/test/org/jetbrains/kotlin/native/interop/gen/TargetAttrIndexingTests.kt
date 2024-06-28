/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TargetAttrIndexingTests : IndexerTestsBase() {
    // Note: these tests are actually target-specific:
    // When compiling for aarch64, target attribute seems to accept any nonsense.
    // With x86_64, attributes with unknown target are ignored.
    // It makes sense to make this test parameterized with compilation target.
    //
    // Currently, the test uses `aes` value for target attribute, because it seems to make sense for both platforms.

    @Test
    fun `regular function`() {
        val function = indexFunction("""
            void foo(void);
            """.trimIndent()
        )
        assertEquals("foo", function.name)
    }

    @Test
    fun `function with other attribute`() {
        val function = indexFunction("""
            __attribute__((pure))
            void foo(void);
            """.trimIndent()
        )
        assertEquals("foo", function.name)
    }

    @Test
    fun `__target__ function`() {
        assertNull(indexFunctionOrNull("""
            __attribute__((__target__("aes")))
            void foo(void);
            """.trimIndent()
        ))
    }

    @Test
    fun `target function`() {
        assertNull(indexFunctionOrNull("""
            __attribute__((target("aes")))
            void foo(void);
            """.trimIndent()
        ))
    }

    @Test
    fun `define __target__ function`() {
        assertNull(indexFunctionOrNull("""
            #define AES_AI __attribute__((__target__("aes"), always_inline))
            AES_AI void foo(void);
            """.trimIndent()
        ))
    }

    @Test
    fun `define target function`() {
        assertNull(indexFunctionOrNull("""
            #define AI_AES __attribute__((always_inline, target("aes")))
            AI_AES void foo(void);
            """.trimIndent())
        )
    }

    @Test
    fun `function __target__`() {
        assertNull(indexFunctionOrNull("""
            void foo(void) __attribute__((__target__("sse4")));
            """.trimIndent())
        )
    }

    @Test
    fun `target nonsense function`() {
        // When compiling for aarch64, target attribute seems to accept any nonsense.
        Assume.assumeFalse(HostManager.hostArch() == "aarch64")

        val function = indexFunction("""
            __attribute__((target("nonsense")))
            void foo(void);
            """.trimIndent()
        )
        assertEquals("foo", function.name)
    }

    @Test
    fun `__target__ nonsense function`() {
        // When compiling for aarch64, target attribute seems to accept any nonsense.
        Assume.assumeFalse(HostManager.hostArch() == "aarch64")

        val function = indexFunction("""
            __attribute__((__target__("nonsense")))
            void foo(void);
            """.trimIndent()
        )
        assertEquals("foo", function.name)
    }
}
