/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.utils.isParentOf
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafePathUtilsTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `isParentOf strict`() {
        assertTrue(tempDir.isParentOf(tempDir))
        assertFalse(tempDir.isParentOf(tempDir, strict = true))
        assertTrue(tempDir.isParentOf(tempDir.resolve("x"), strict = true))
    }
}
