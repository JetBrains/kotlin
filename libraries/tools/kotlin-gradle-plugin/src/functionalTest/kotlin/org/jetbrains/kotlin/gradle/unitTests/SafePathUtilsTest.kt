/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.utils.ensurePathComponentSafeString
import org.jetbrains.kotlin.gradle.utils.isParentOf
import org.jetbrains.kotlin.gradle.utils.isPathComponentSafe
import org.jetbrains.kotlin.gradle.utils.resolveInside
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafePathUtilsTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `path safe strings`() {
        listOf(
            "org.jetbrains.sample-sampleLibrary",
            "1.0.0-SNAPSHOT",
            "appleAndLinuxMain",
            "Vk5pxQ_-",
            "a..b",
            "lib.",
        ).forEach { assertTrue(it.isPathComponentSafe, "'$it' is expected to be path safe") }

        listOf(
            "",
            ".",
            "..",
            ".hidden",
            "../evil",
            "a/b",
            "a\\b",
            "..\\evil",
            "C:evil",
            "a b",
            "a\u0000b",
        ).forEach { assertFalse(it.isPathComponentSafe, "'$it' is expected to be NOT path safe") }

        assertFailsWith<IllegalArgumentException> { ensurePathComponentSafeString("..") }
        assertEquals("1.0.0", ensurePathComponentSafeString("1.0.0"))
    }

    @Test
    fun resolveInside() {
        assertEquals(tempDir.resolve("a/b.klib"), tempDir.resolveInside("a/b.klib"))
        assertEquals(tempDir.resolve("a/../b"), tempDir.resolveInside(File("a/../b")))

        listOf("..", ".", "", "../x", "a/../../x", tempDir.parentFile.resolve("x").absolutePath).forEach { path ->
            assertFailsWith<IllegalArgumentException>("'$path' is expected to be rejected") { tempDir.resolveInside(path) }
        }
    }

    @Test
    fun `isParentOf strict`() {
        assertTrue(tempDir.isParentOf(tempDir))
        assertFalse(tempDir.isParentOf(tempDir, strict = true))
        assertTrue(tempDir.isParentOf(tempDir.resolve("x"), strict = true))
    }
}
