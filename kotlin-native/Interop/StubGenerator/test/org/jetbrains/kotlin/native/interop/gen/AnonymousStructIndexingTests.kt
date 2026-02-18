/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnonymousStructIndexingTests : IndexerTestsBase() {
    @Test
    fun `named struct`() {
        val struct = indexStruct("""
            struct S { int x; };
        """.trimIndent())
        assertFalse(struct.isAnonymous)
        assertEquals("struct S", struct.spelling)
    }

    @Test
    fun `named union`() {
        val struct = indexStruct("""
            union U { int x; };
        """.trimIndent())
        assertFalse(struct.isAnonymous)
        assertEquals("union U", struct.spelling)
    }

    @Test
    fun `typedef struct`() {
        val struct = indexStruct("""
            typedef struct { int x; } S;
        """.trimIndent())
        assertFalse(struct.isAnonymous)
        assertEquals("S", struct.spelling)
    }

    @Test
    fun `unnamed struct global`() {
        val struct = indexStruct("""
            struct { int x; } global;
        """.trimIndent())
        assertTrue(struct.isAnonymous)
    }

    @Test
    fun `unnamed union global`() {
        val struct = indexStruct("""
            union { int x; int y; } global;
        """.trimIndent())
        assertTrue(struct.isAnonymous)
    }

    @Test
    fun `nested anonymous struct`() {
        val struct = indexStruct("""
            struct S {
                struct {
                    int x;
                };
            };
        """.trimIndent())
        // ^ This also checks that the nested struct is not even a StructDecl.
        assertFalse(struct.isAnonymous)
        assertEquals("struct S", struct.spelling)
    }

    @Test
    fun `nested unnamed struct`() {
        val structs = indexStructs("""
            struct S {
                struct {
                    int x;
                } nested;
            };
        """.trimIndent())
        val (nested, s) = structs.partition { it.isAnonymous }
        assertEquals("struct S", s.single().spelling)
        assertTrue(nested.single().isAnonymous)
    }

    @Test
    fun `type definition lookups - anonymous structs don't get incorrectly conflated`() {
        val structs = indexStructs("""
            struct { int i; } a;
            struct { int i; } b;
        """.trimIndent()).toList()
        assertEquals(
                listOf(true, true),
                structs.map { it.isAnonymous },
        )
        assertNotNull(structs[0].def)
        assertNotNull(structs[1].def)
        assertNotEquals(structs[0].def, structs[1].def)
    }
}
