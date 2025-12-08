/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ProtocolIndexingTests : IndexerTestsBase() {
    @BeforeEach
    fun onlyOnMac() {
        Assumptions.assumeTrue(HostManager.hostIsMac)
    }

    @Test
    fun `default binary name`() {
        val protocol = indexProtocol("""
            @protocol P
            @end
        """.trimIndent())

        assertEquals("P", protocol.binaryName)
    }

    @Test
    fun `default binary name with forward declaration`() {
        val protocol = indexProtocol("""
            @protocol P;
            @protocol P
            @end
        """.trimIndent())

        assertEquals("P", protocol.binaryName)
    }

    @Test
    fun `custom binary name`() {
        val protocol = indexProtocol("""
            __attribute__((objc_runtime_name("P2")))
            @protocol P
            @end
        """.trimIndent())

        assertEquals("P2", protocol.binaryName)
    }

    @Test
    fun `custom binary name on forward declaration`() {
        val protocol = indexProtocol("""
            __attribute__((objc_runtime_name("P2")))
            @protocol P;

            @protocol P
            @end
        """.trimIndent())

        assertEquals("P", protocol.binaryName)
    }

    @Test
    fun `custom binary name on definition after declaration`() {
        val protocol = indexProtocol("""
            @protocol P;

            __attribute__((objc_runtime_name("P2")))
            @protocol P
            @end
        """.trimIndent())

        assertEquals("P2", protocol.binaryName)
    }

    @Test
    fun `same custom binary name on forward declaration and definition`() {
        val protocol = indexProtocol("""
            __attribute__((objc_runtime_name("P2")))
            @protocol P;

            __attribute__((objc_runtime_name("P2")))
            @protocol P
            @end
        """.trimIndent())

        assertEquals("P2", protocol.binaryName)
    }

    @Test
    fun `different custom binary names on forward declaration and definition`() {
        val protocol = indexProtocol("""
            __attribute__((objc_runtime_name("P1")))
            @protocol P;

            __attribute__((objc_runtime_name("P2")))
            @protocol P
            @end
        """.trimIndent())

        assertEquals("P2", protocol.binaryName)
    }

    @Test
    fun `different custom binary names on definition and declaration`() {
        val protocol = indexProtocol("""
            __attribute__((objc_runtime_name("P2")))
            @protocol P
            @end

            __attribute__((objc_runtime_name("P1")))
            @protocol P;
        """.trimIndent())

        assertEquals("P2", protocol.binaryName)
    }

    @Test
    fun `custom binary name on declaration after definition`() {
        val protocol = indexProtocol("""
            @protocol P
            @end

            __attribute__((objc_runtime_name("P1")))
            @protocol P;
        """.trimIndent())

        assertEquals("P", protocol.binaryName)
    }

    @Test
    fun `custom binary name on definition before declaration`() {
        val protocol = indexProtocol("""
            __attribute__((objc_runtime_name("P2")))
            @protocol P
            @end

            @protocol P;
        """.trimIndent())

        assertEquals("P2", protocol.binaryName)
    }
}
