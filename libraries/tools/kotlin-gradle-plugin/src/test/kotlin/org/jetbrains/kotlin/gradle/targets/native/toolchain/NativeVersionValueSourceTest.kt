/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.jetbrains.kotlin.gradle.testing.WithTemporaryFolder
import org.jetbrains.kotlin.gradle.testing.newTempDirectory
import org.junit.jupiter.api.io.TempDir
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeVersionValueSourceTest : WithTemporaryFolder {

    @field:TempDir
    override lateinit var temporaryFolder: Path

    @Test
    fun testMoveToNonEmptyDir() {
        val nativeDir = newTempDirectory().resolve("native_dir").also { it.createDirectories() }
        val versionDir = nativeDir.resolve("version").also { it.createDirectory() }
        versionDir.resolve("A.kt").createFile()
        versionDir.resolve("C.kt").createFile()

        NativeVersionValueSource.copyNativeBundleDistribution(createTarGz(), versionDir.toFile())
        assertEquals("class A {}", versionDir.resolve("A.kt").toFile().readText())
        assertTrue("File B.kt should be copied from directory") { versionDir.resolve("B.kt").exists() }
        assertTrue("Marker file should be created") { versionDir.resolve(NativeVersionValueSource.Companion.MARKER_FILE).exists() }
    }

    private fun createTarGz(): File {
        val tarFile = newTempDirectory().resolve("version.tar.gz").toFile()
        TarArchiveOutputStream(
            GZIPOutputStream(
                BufferedOutputStream(
                    FileOutputStream(tarFile)
                )
            )
        ).use {
            val fileContents = "class A {}".toByteArray()
            val entry = TarArchiveEntry("version/A.kt")
            entry.size = fileContents.size.toLong()
            it.putArchiveEntry(entry)
            it.write(fileContents)
            it.closeArchiveEntry()
            it.putArchiveEntry(TarArchiveEntry("version/B.kt"))
            it.closeArchiveEntry()
        }
        return tarFile
    }
}