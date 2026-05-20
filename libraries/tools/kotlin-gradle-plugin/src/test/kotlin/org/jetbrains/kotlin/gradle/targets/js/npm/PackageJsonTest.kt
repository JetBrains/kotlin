/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.FileSystemException

class PackageJsonTest {

    @Test
    fun `saveTo non-existing directory fails`(@TempDir tempDir: File) {
        val blockedParent = tempDir.resolve("blocked")
        blockedParent.createNewFile() // prevents createDirectories()

        val target = blockedParent.resolve("nested/package.json")

        val pj = PackageJson(name = "foo", version = "1.0.0")

        val exception = assertThrows<FileSystemException> {
            pj.saveTo(target)
        }

        assertEquals(
            "${target.parent}: Not a directory",
            exception.message
        )

        assertFalse(target.parentFile.exists(), "Expect parent directory was not created.")
    }
}
