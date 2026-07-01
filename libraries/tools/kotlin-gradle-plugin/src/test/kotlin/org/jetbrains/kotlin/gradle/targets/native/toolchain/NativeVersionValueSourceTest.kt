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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativeVersionValueSourceTest : WithTemporaryFolder {

    @field:TempDir
    override lateinit var temporaryFolder: Path

    /**
     * Fresh install: target directory does not exist yet.
     * Expects atomic rename into place; directory and marker appear together.
     */
    @Test
    fun testInstallIntoFreshDir() {
        val nativeDir = newTempDirectory().resolve("native_dir").also { it.createDirectories() }
        val versionDir = nativeDir.resolve("version") // does NOT exist yet

        NativeVersionValueSource.extractNativeBundleDistribution(createTarGz(), versionDir.toFile())

        assertTrue("Version dir should be created") { versionDir.exists() }
        assertEquals("class A {}", versionDir.resolve("A.kt").toFile().readText())
        assertTrue("File B.kt should be extracted") { versionDir.resolve("B.kt").exists() }
        assertTrue("Marker file should be created") { versionDir.resolve(NativeVersionValueSource.MARKER_FILE).exists() }
    }

    /**
     * Stale install: target directory exists but has no marker (interrupted/partial previous run).
     * Expects the stale directory to be atomically replaced; files from the stale run
     * that are not in the archive (C.kt) must not survive.
     */
    @Test
    fun testReplaceStaleDir() {
        val nativeDir = newTempDirectory().resolve("native_dir").also { it.createDirectories() }
        val versionDir = nativeDir.resolve("version").also { it.createDirectory() }
        // Stale file from a previous interrupted install; not in the archive.
        versionDir.resolve("C.kt").createFile()
        // A.kt exists but with different content.
        versionDir.resolve("A.kt").createFile()
        // No marker file → treated as stale.

        NativeVersionValueSource.extractNativeBundleDistribution(createTarGz(), versionDir.toFile())

        assertEquals("class A {}", versionDir.resolve("A.kt").toFile().readText())
        assertTrue("File B.kt should be present after replace") { versionDir.resolve("B.kt").exists() }
        assertTrue("Marker file should be created") { versionDir.resolve(NativeVersionValueSource.MARKER_FILE).exists() }
        assertFalse("Stale file C.kt must not survive after atomic replace") { versionDir.resolve("C.kt").exists() }
    }

    /**
     * Already-installed bundle: marker is present.
     * Nothing should happen: existing files stay and the archive is not extracted.
     */
    @Test
    fun testSkipWhenAlreadyInstalled() {
        val nativeDir = newTempDirectory().resolve("native_dir").also { it.createDirectories() }
        val versionDir = nativeDir.resolve("version").also { it.createDirectory() }
        versionDir.resolve("C.kt").createFile()
        // A generated compiler cache from a previous build. It lives inside the bundle dir and must
        // never be deleted while the bundle is installed (has a marker). KT-86251.
        val generatedCache = versionDir.resolve("klib/cache/foo-cache/bin/libfoo-cache.a")
        generatedCache.parent.createDirectories()
        generatedCache.createFile()
        versionDir.resolve(NativeVersionValueSource.MARKER_FILE).createFile()

        NativeVersionValueSource.extractNativeBundleDistribution(createTarGz(), versionDir.toFile())

        assertTrue("Existing file should be preserved") { versionDir.resolve("C.kt").exists() }
        assertTrue("Generated caches must never be deleted for an installed bundle") { generatedCache.exists() }
        assertFalse("Archive content should not overwrite an installed bundle") { versionDir.resolve("B.kt").exists() }
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