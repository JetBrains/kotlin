/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

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
        // Create a file instead of a directory, so createDirectories() will fail
        blockedParent.createNewFile()

        val target = blockedParent.resolve("nested/package.json")

        val pj = PackageJson(name = "foo", version = "1.0.0")

        assertThrows<FileSystemException> {
            pj.saveTo(target)
        }

        assertFalse(target.parentFile.exists(), "Expect parent directory was not created.")
    }
}
