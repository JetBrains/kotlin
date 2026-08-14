/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertNull
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

    @Test
    fun `saveTo creates missing parent directories`(@TempDir tempDir: File) {
        val target = tempDir.resolve("a/b/c/package.json")

        PackageJson(name = "foo", version = "1.0.0").saveTo(target)

        assertTrue(target.isFile, "Expected $target to be a file.")
        val parsed = fromSrcPackageJson(target)
        assertEquals("foo", parsed?.name)
    }

    @Test
    fun `saveTo rewrites an unparseable existing file`(@TempDir tempDir: File) {
        val target = tempDir.resolve("package.json")
        // what an interrupted write leaves behind
        target.writeText("{\"name\": \"fo")

        PackageJson(name = "foo", version = "1.0.0").saveTo(target)

        assertEquals("foo", fromSrcPackageJson(target)?.name)
    }

    @Test
    fun `saveTo reads back a file with a byte order mark`(@TempDir tempDir: File) {
        val target = tempDir.resolve("package.json")
        PackageJson(name = "foo", version = "1.0.0").saveTo(target)
        target.writeText("\uFEFF" + target.readText())
        val stamp = target.lastModified()

        PackageJson(name = "foo", version = "1.0.0").saveTo(target)

        assertEquals(stamp, target.lastModified(), "Expected an unchanged package.json to not be rewritten.")
    }

    @Test
    fun `a package json without a name is ignored`(@TempDir tempDir: File) {
        val target = tempDir.resolve("package.json")
        target.writeText("{\"version\": \"1.0.0\"}")

        assertNull(fromSrcPackageJson(target))
    }

    @Test
    fun `moduleName falls back to the directory name`(@TempDir tempDir: File) {
        val directory = tempDir.resolve("some-module").also { it.mkdirs() }
        // a private package may legitimately have no "name"
        directory.resolve("package.json").writeText("{\"private\": true}")

        assertEquals("some-module", moduleName(directory))
    }

    @Test
    fun `customField rejects a value with no JSON representation`(@TempDir tempDir: File) {
        val target = tempDir.resolve("package.json")
        val pj = PackageJson(name = "foo", version = "1.0.0")
        pj.customField("broken", Any())

        assertThrows<IllegalArgumentException> {
            pj.saveTo(target)
        }
    }
}
