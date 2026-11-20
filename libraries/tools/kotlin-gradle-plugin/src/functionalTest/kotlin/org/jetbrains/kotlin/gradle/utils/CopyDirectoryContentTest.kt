/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.internal.extensions.core.serviceOf
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.zipTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CopyDirectoryContentTest {

    private val project = buildProject()
    private val fileOperations = project.serviceOf<FileSystemOperations>()

    @Test
    fun copiesArchiveDirectoryContentsToDestinationWithoutTopLevelDirectory(
        @TempDir tempDir: File,
    ) {
        val archiveContent = tempDir.resolve("archive-content/wit/nested").apply { mkdirs() }
        archiveContent.resolve("sample.wit").writeText("package sample;\n")
        val archive = tempDir.resolve("wit.jar").also { zipTo(it, tempDir.resolve("archive-content")) }
        val witFiles = project.serviceOf<ArchiveOperations>().zipTree(archive)
        val destination = tempDir.resolve("archive-output")

        assertFalse(destination.exists())

        fileOperations.copySubDirectoryContent(witFiles, "wit", destination)

        assertEquals("package sample;\n", destination.resolve("nested/sample.wit").readText())
        assertFalse(destination.resolve("wit").exists())
    }

    @Test
    fun copiesFileSystemDirectoryContentsAndSkipsEmptyDirectories(
        @TempDir tempDir: File,
    ) {
        val source = tempDir.resolve("source").apply { mkdirs() }
        source.resolve("wit/nested/sample.wit").apply {
            parentFile.mkdirs()
            writeText("package filesystem;\n")
        }
        source.resolve("wit/empty").mkdirs()
        val witFiles = project.objects.fileTree().apply { setDir(source) }
        val destination = tempDir.resolve("filesystem-output")

        fileOperations.copySubDirectoryContent(witFiles, "wit", destination)

        assertEquals("package filesystem;\n", destination.resolve("nested/sample.wit").readText())
        assertFalse(destination.resolve("wit").exists())
        assertFalse(destination.resolve("empty").exists())
        assertTrue(destination.isDirectory)
    }
}
