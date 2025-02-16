/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.indexer.*
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ObjCTypeIndexingTest : IndexerTestsBase() {
    @Before
    fun onlyOnMac() {
        Assume.assumeTrue(HostManager.hostIsMac)
    }

    private fun type(type: String, typeDeclarations: String = ""): Type {
        val headerContents = buildString {
            appendLine(typeDeclarations)
            // Note: this won't work for things like block types, but it doesn't matter yet.
            append("$type x;")
        }
        val index = index(headerContents, language = Language.OBJECTIVE_C)
        val global = index.index.globals.single()
        assertEquals("x", global.name)
        return global.type
    }

    private fun assertProtocols(type: ObjCQualifiedPointer, vararg expectedProtocols: String) {
        val actualProtocols = type.protocols.map { it.name }
        assertEquals(expectedProtocols.toList(), actualProtocols)
    }

    @Test
    fun id() {
        val actual = type("id")
        val expected = ObjCIdType(ObjCPointer.Nullability.Unspecified, emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun `id with one protocol`() {
        val actual = type("id<P>", "@protocol P;")
        assertIs<ObjCIdType>(actual)
        assertEquals(ObjCPointer.Nullability.Unspecified, actual.nullability)
        assertProtocols(actual, "P")
    }

    @Test
    fun `id with two protocols`() {
        val actual = type("id<Q, P>",
                """
                    @protocol P;
                    @protocol Q
                    @end
                """.trimIndent())
        assertIs<ObjCIdType>(actual)
        assertEquals(ObjCPointer.Nullability.Unspecified, actual.nullability)
        assertProtocols(actual, "Q", "P")
    }

    @Test
    fun `id with one protocol twice`() {
        val actual = type("id<P, P>",
                """
                    @protocol P
                    @end
                """.trimIndent())
        assertIs<ObjCIdType>(actual)
        assertEquals(ObjCPointer.Nullability.Unspecified, actual.nullability)
        assertProtocols(actual, "P", "P")
    }

    @Test
    fun `interface`() {
        val actual = type("C*",
                """
                    @interface C
                    @end
                """.trimIndent())
        assertIs<ObjCObjectPointer>(actual)
        assertEquals("C", actual.def.name)
        assertFalse(actual.def.isForwardDeclaration)
        assertEquals(ObjCPointer.Nullability.Unspecified, actual.nullability)
        assertProtocols(actual, )
    }

    @Test
    fun `class forward declaration with one protocol`() {
        val actual = type("C<P>*",
                """
                    @class C;
                    @protocol P;
                """.trimIndent())
        assertIs<ObjCObjectPointer>(actual)
        assertEquals("C", actual.def.name)
        assertTrue(actual.def.isForwardDeclaration)
        assertEquals(ObjCPointer.Nullability.Unspecified, actual.nullability)
        assertProtocols(actual, "P")
    }

    @Test
    fun `typedef interface`() {
        val actual = type("D*",
                """
                    @class C;
                    typedef C D;
                """.trimIndent())
        assertIs<ObjCObjectPointer>(actual)
        assertEquals("C", actual.def.name)
        assertEquals(ObjCPointer.Nullability.Unspecified, actual.nullability)
        assertProtocols(actual, )
    }

    @Test
    fun `typedef interface with protocol`() {
        val actual = type("D<P>*",
                """
                    @interface C
                    @end
                    @protocol P
                    @end
                    typedef C D;
                """.trimIndent())
        assertIs<ObjCObjectPointer>(actual)
        assertEquals("C", actual.def.name)
        assertEquals(ObjCPointer.Nullability.Unspecified, actual.nullability)
        assertProtocols(actual, "P")
    }

    @Test
    fun `typedef interface with protocol under typedef`() {
        val actual = type("D*",
                """
                    @interface C
                    @end
                    @protocol P
                    @end
                    typedef C<P> D;
                """.trimIndent())
        assertIs<ObjCObjectPointer>(actual)
        assertEquals("C", actual.def.name)
        assertEquals(ObjCPointer.Nullability.Unspecified, actual.nullability)
        assertProtocols(actual, "P")
    }

    @Test
    fun `typedef interface with protocol also under typedef`() {
        val actual = type("D<Q>*",
                """
                    @interface C
                    @end
                    @protocol P
                    @end
                    @protocol Q
                    @end
                    typedef C<P> D;
                """.trimIndent())
        assertIs<ObjCObjectPointer>(actual)
        assertEquals("C", actual.def.name)
        assertEquals(ObjCPointer.Nullability.Unspecified, actual.nullability)
        assertProtocols(actual, "Q")
    }

    @Test
    fun Class() {
        val actual = type("Class")
        val expected = ObjCClassPointer(ObjCPointer.Nullability.Unspecified, emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun `Class with one protocol`() {
        val actual = type("Class<P>", "@protocol P;")
        // That's what should happen:
        //   assertIs<ObjCClassPointer>(actual)
        // But it doesn't, because of https://youtrack.jetbrains.com/issue/KT-56860.
        // Instead, we have
        assertIs<ObjCIdType>(actual)

        assertEquals(ObjCPointer.Nullability.Unspecified, actual.nullability)
        assertProtocols(actual, "P")
    }
}