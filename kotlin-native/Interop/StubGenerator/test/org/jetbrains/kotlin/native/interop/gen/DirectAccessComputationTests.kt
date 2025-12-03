/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.indexer.DirectAccess
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DirectAccessComputationTests : IndexerTestsBase() {
    private val DirectAccess.symbol: String?
        get() = when (this) {
            is DirectAccess.Symbol -> this.name
            is DirectAccess.Unavailable -> null
        }

    private val DirectAccess.unmangledSymbol: String?
        get() {
            val symbol = this.symbol ?: return null
            val prefix = if (HostManager.hostIsMac) "_" else ""
            assertTrue(symbol.startsWith(prefix)) { """"$symbol" must start with "$prefix"""" }
            return symbol.removePrefix(prefix)
        }

    @Test
    fun `default global symbol name`() {
        val decl = index("""
            extern int foo;
        """.trimIndent()).global

        assertEquals("foo", decl.directAccess.unmangledSymbol)
    }

    @Test
    fun `global with __asm`() {
        val decl = index("""
            extern int foo __asm("bar");
        """.trimIndent()).global

        assertEquals("bar", decl.directAccess.symbol)
    }

    @Test
    fun `global defined in header`() {
        val decl = index("""
            static int foo = 42;
        """.trimIndent()).global

        // TODO KT-83039: should be unavailable.
        assertEquals("foo", decl.directAccess.unmangledSymbol)
    }

    @Test
    fun `global declared and defined in header`() {
        val decl = index("""
            static int foo;
            static int foo = 42;
        """.trimIndent()).global

        // TODO KT-83039: should be unavailable.
        assertEquals("foo", decl.directAccess.unmangledSymbol)
    }

    @Test
    fun `global defined and declared in header`() {
        val decl = index("""
            static int foo = 42;
            static int foo;
        """.trimIndent()).global

        // TODO KT-83039: should be unavailable.
        assertEquals("foo", decl.directAccess.unmangledSymbol)
    }

    @Test
    fun `global defined in def file`() {
        val decl = index(
                headerContents = "",
                appendDefFile = """
                    ---
                    static int foo = 42;
                """.trimIndent()
        ).global

        // TODO KT-83039: should be unavailable.
        assertEquals("foo", decl.directAccess.unmangledSymbol)
    }

    @Test
    fun `global declared in header and defined in def file`() {
        val decl = index(
                headerContents = """
                    static int foo;
                """.trimIndent(),
                appendDefFile = """
                    ---
                    static int foo = 42;
                """.trimIndent()
        ).global

        // TODO KT-83039: should be unavailable.
        assertEquals("foo", decl.directAccess.unmangledSymbol)
    }

    @Test
    fun `global defined in header and declared in def file`() {
        val decl = index(
                headerContents = """
                    static int foo = 42;
                """.trimIndent(),
                appendDefFile = """
                    ---
                    static int foo;
                """.trimIndent()
        ).global

        // TODO KT-83039: should be unavailable.
        assertEquals("foo", decl.directAccess.unmangledSymbol)
    }

    @Test
    fun `const global defined in header`() {
        val decl = index("""
            static const int foo = 42;
        """.trimIndent()).global

        // TODO KT-83039: should be unavailable.
        assertEquals("foo", decl.directAccess.unmangledSymbol)
    }
}
