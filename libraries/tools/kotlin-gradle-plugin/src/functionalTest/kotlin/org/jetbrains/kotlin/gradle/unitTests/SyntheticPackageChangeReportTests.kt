/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SyntheticPackageChangeReport
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyntheticPackageChangeReportTests {

    @Test
    fun `diff returns empty changes when snapshots are identical`(@TempDir tempDir: Path) {
        val root = tempDir.toFile()
        writeFile(root, "Package.swift", "// content")

        val before = SyntheticPackageChangeReport.snapshot(root, listOf(root.resolve("Package.swift")))
        val after = SyntheticPackageChangeReport.snapshot(root, listOf(root.resolve("Package.swift")))

        val changes = SyntheticPackageChangeReport.diff(before, after)

        assertTrue(changes.isEmpty)
        assertEquals(emptyList(), changes.added)
        assertEquals(emptyList(), changes.removed)
        assertEquals(emptyList(), changes.modified)
    }

    @Test
    fun `diff detects added files`(@TempDir tempDir: Path) {
        val root = tempDir.toFile()
        writeFile(root, "Package.swift", "// content")

        val before = SyntheticPackageChangeReport.snapshot(root, listOf(root.resolve("Package.swift")))
        writeFile(root, "Sources/Foo/Foo.m", "")
        val after = SyntheticPackageChangeReport.snapshot(
            root,
            listOf(root.resolve("Package.swift"), root.resolve("Sources/Foo/Foo.m")),
        )

        val changes = SyntheticPackageChangeReport.diff(before, after)

        assertFalse(changes.isEmpty)
        assertEquals(listOf("Sources/Foo/Foo.m"), changes.added)
        assertEquals(emptyList(), changes.removed)
        assertEquals(emptyList(), changes.modified)
    }

    @Test
    fun `diff detects removed files`(@TempDir tempDir: Path) {
        val root = tempDir.toFile()
        writeFile(root, "Package.swift", "// content")
        writeFile(root, "Sources/Foo/Foo.m", "")

        val before = SyntheticPackageChangeReport.snapshot(
            root,
            listOf(root.resolve("Package.swift"), root.resolve("Sources/Foo/Foo.m")),
        )
        val after = SyntheticPackageChangeReport.snapshot(root, listOf(root.resolve("Package.swift")))

        val changes = SyntheticPackageChangeReport.diff(before, after)

        assertEquals(emptyList(), changes.added)
        assertEquals(listOf("Sources/Foo/Foo.m"), changes.removed)
        assertEquals(emptyList(), changes.modified)
    }

    @Test
    fun `diff detects modified files`(@TempDir tempDir: Path) {
        val root = tempDir.toFile()
        val pkg = writeFile(root, "Package.swift", "// content")

        val before = SyntheticPackageChangeReport.snapshot(root, listOf(pkg))
        pkg.writeText("// content v2")
        val after = SyntheticPackageChangeReport.snapshot(root, listOf(pkg))

        val changes = SyntheticPackageChangeReport.diff(before, after)

        assertEquals(emptyList(), changes.added)
        assertEquals(emptyList(), changes.removed)
        assertEquals(listOf("Package.swift"), changes.modified)
    }

    @Test
    fun `diff sorts each bucket alphabetically and reports all bucket kinds together`(@TempDir tempDir: Path) {
        val root = tempDir.toFile()
        val keep = writeFile(root, "subpackages/A/Package.swift", "a")
        val toRemove = writeFile(root, "subpackages/B/Package.swift", "b")
        val toModify = writeFile(root, "Package.swift", "main")

        val before = SyntheticPackageChangeReport.snapshot(root, listOf(keep, toRemove, toModify))

        // mutate filesystem
        toRemove.delete()
        toModify.writeText("main v2")
        val toAddLater = writeFile(root, "subpackages/Z/Package.swift", "z")
        val toAddFirst = writeFile(root, "subpackages/A2/Package.swift", "a2")

        val after = SyntheticPackageChangeReport.snapshot(root, listOf(keep, toModify, toAddLater, toAddFirst))

        val changes = SyntheticPackageChangeReport.diff(before, after)

        assertEquals(listOf("subpackages/A2/Package.swift", "subpackages/Z/Package.swift"), changes.added)
        assertEquals(listOf("subpackages/B/Package.swift"), changes.removed)
        assertEquals(listOf("Package.swift"), changes.modified)
    }

    @Test
    fun `render produces stable error-prefixed output for non-empty changes`() {
        val rendered = SyntheticPackageChangeReport.render(
            SyntheticPackageChangeReport.Changes(
                added = listOf("Sources/Foo/Foo.m"),
                removed = listOf("subpackages/Old/Package.swift"),
                modified = listOf("Package.swift"),
            )
        )

        val expected = """
            error: Synthetic linkage package files changed during the build:
            error:   + Sources/Foo/Foo.m (added)
            error:   - subpackages/Old/Package.swift (removed)
            error:   ~ Package.swift (modified)
        """.trimIndent()
        assertEquals(expected, rendered)
    }

    @Test
    fun `render returns empty string when no changes`() {
        val rendered = SyntheticPackageChangeReport.render(
            SyntheticPackageChangeReport.Changes(
                added = emptyList(),
                removed = emptyList(),
                modified = emptyList(),
            )
        )
        assertEquals("", rendered)
    }

    private fun writeFile(root: File, relative: String, content: String): File {
        val file = root.resolve(relative)
        file.parentFile.mkdirs()
        file.writeText(content)
        return file
    }
}
