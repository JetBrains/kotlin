/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling

import kotlin.test.*

class SchemaVersionTest {

    @Test
    fun `compare same version`() {
        assertTrue(SchemaVersion(1, 0, 0) <= SchemaVersion(1, 0, 0), "Expected 1.0.0 <= 1.0.0")
        assertTrue(SchemaVersion(1, 0, 0) >= SchemaVersion(1, 0, 0), "Expected 1.0.0 >= 1.0.0")
        assertFalse(SchemaVersion(1, 0, 0) > SchemaVersion(1, 0, 0), "*NOT* Expected 1.0.0 > 1.0.0")
    }

    @Test
    fun `compare higher version`() {
        assertTrue(SchemaVersion(2, 2, 2) > SchemaVersion(2, 2, 1))
        assertTrue(SchemaVersion(2, 2, 2) > SchemaVersion(2, 1, 2))
        assertTrue(SchemaVersion(2, 2, 2) > SchemaVersion(1, 3, 3))
    }

    @Test
    fun `compare lower version`() {
        assertTrue(SchemaVersion(2, 2, 2) < SchemaVersion(2, 2, 3))
        assertTrue(SchemaVersion(2, 2, 2) < SchemaVersion(2, 3, 1))
        assertTrue(SchemaVersion(2, 2, 2) < SchemaVersion(3, 1, 1))
    }

    @Test
    fun parseSchemaVersion() {
        assertEquals(
            SchemaVersion(2, 15, 100), SchemaVersion.parseStringOrThrow("2.15.100")
        )

        assertEquals(
            SchemaVersion(0, 0, 0), SchemaVersion.parseStringOrThrow("0.0.0")
        )

        assertEquals(
            SchemaVersion(1, 0, 0), SchemaVersion.parseStringOrThrow("1.0.0")
        )
    }

    @Test
    fun `parseSchemaVersion failure`() {
        assertFailsWith<IllegalArgumentException> { SchemaVersion.parseStringOrThrow("1.0") }
        assertFailsWith<IllegalArgumentException> { SchemaVersion.parseStringOrThrow("1.0.0.0") }
        assertFailsWith<IllegalArgumentException> { SchemaVersion.parseStringOrThrow("a.b.c") }
    }

    @Test
    fun `toString and parse`() {
        assertEquals(
            SchemaVersion(1, 0, 0), SchemaVersion.parseStringOrThrow(SchemaVersion(1, 0, 0).toString())
        )

        assertEquals(
            SchemaVersion(28, 100, 900), SchemaVersion.parseStringOrThrow(SchemaVersion(28, 100, 900).toString())
        )
    }

    @Test
    fun isCompatible() {
        assertTrue(SchemaVersion(1, 1, 0).isCompatible(SchemaVersion(1, 0, 0)))
        assertTrue(SchemaVersion(1, 0, 0).isCompatible(SchemaVersion(1, 0, 0)))
        assertFalse(SchemaVersion(1, 0, 0).isCompatible(SchemaVersion(1, 1, 0)))
        assertFalse(SchemaVersion(1, 0, 0).isCompatible(SchemaVersion(2, 0, 0)))
        assertFalse(SchemaVersion(2, 0, 0).isCompatible(SchemaVersion(1, 0, 0)))
    }
}
