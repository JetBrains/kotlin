/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.jetbrains.kotlin.gradle.testing.WithTemporaryFolder
import org.jetbrains.kotlin.gradle.utils.TarExtractionSecurityException
import org.jetbrains.kotlin.gradle.utils.unzipTarGz
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.zip.GZIPOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinNativeBundleTarExtractionSecurityTest : WithTemporaryFolder {

    @field:TempDir
    override lateinit var temporaryFolder: Path

    // CWE-22: entry name with ../ escapes targetDir.
    @Test
    fun `path traversal entry is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val escapedFile = temporaryFolder.resolve("tar_traversal_KT86605.txt")
        val archive = createTarGz {
            directory("kotlin-native/")
            file("kotlin-native/../../tar_traversal_KT86605.txt", "traversal")
        }

        assertThrows<TarExtractionSecurityException> {
            archive.toPath().unzipTarGz(targetDir)
        }
        assertFalse(escapedFile.exists(), "Entry escaped targetDir: $escapedFile")
    }

    // KT-87190: symlinks pointing outside targetBase (e.g. xcode-addon bin/altool)
    // must extract without error — creating one doesn't write data outside.
    @Test
    fun `escaping symlink is allowed`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val outside = temporaryFolder.resolve("outside").createDirectories()
        val archive = createTarGz {
            directory("bin/")
            symlink("bin/altool", "../../../outside/altoolShim")
        }

        archive.toPath().unzipTarGz(targetDir)

        val symlink = targetDir.resolve("bin/altool")
        assertTrue(
            Files.exists(symlink, LinkOption.NOFOLLOW_LINKS),
            "Symlink should have been created inside targetDir: $symlink"
        )
        assertFalse(
            outside.resolve("altoolShim").exists(),
            "Nothing should have been written outside targetDir"
        )
    }

    // CWE-59: a regular-file entry at the same path as a previously extracted
    // escaping symlink must be rejected — FileOutputStream follows the link.
    @Test
    fun `file entry overwriting leaf escaping symlink is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val outside = temporaryFolder.resolve("outside").createDirectories()
        val victim = outside.resolve("victim.txt")
        victim.writeText("original")
        val archive = createTarGz {
            directory("bin/")
            symlink("bin/evil", "../outside/victim.txt") // escaping but allowed
            file("bin/evil", "PWNED")                   // same path — must be rejected
        }

        assertThrows<TarExtractionSecurityException> {
            archive.toPath().unzipTarGz(targetDir)
        }
        assertEquals("original", victim.toFile().readText(), "victim.txt must not be overwritten")
    }

    // CWE-59: file entry whose path goes through an escaping directory symlink.
    @Test
    fun `file written through escaping symlink is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val outside = temporaryFolder.resolve("outside").createDirectories()
        val archive = createTarGz {
            directory("bin/")
            symlink("bin/link", "../../outside")
            file("bin/link/payload.txt", "evil")
        }

        assertThrows<TarExtractionSecurityException> {
            archive.toPath().unzipTarGz(targetDir)
        }
        assertFalse(
            outside.resolve("payload.txt").exists(),
            "File must not have been written outside targetDir via symlink"
        )
    }

    // CWE-59: a symlink entry whose path goes through an escaping directory symlink.
    // Creating the symlink would land it outside targetDir via the escaping ancestor.
    @Test
    fun `symlink created through escaping symlink is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val outside = temporaryFolder.resolve("outside").createDirectories()
        val archive = createTarGz {
            directory("bin/")
            symlink("bin/link", "../../outside")
            symlink("bin/link/evil", "/anything")
        }

        assertThrows<TarExtractionSecurityException> {
            archive.toPath().unzipTarGz(targetDir)
        }
        assertFalse(
            outside.resolve("evil").exists(),
            "Symlink must not have been created outside targetDir via symlink"
        )
    }

    // CWE-59: hardlink whose location goes through an escaping symlink.
    // The hardlink target is inside targetBase and passes validateHardlinkTarget,
    // but the link itself would land outside.
    @Test
    fun `hardlink written through escaping symlink is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val outside = temporaryFolder.resolve("outside").createDirectories()
        val source = targetDir.resolve("source.txt")
        source.toFile().writeText("data")
        val archive = createTarGz {
            directory("bin/")
            symlink("bin/link", "../../outside")
            hardlink("bin/link/payload.txt", "source.txt")
        }

        assertThrows<TarExtractionSecurityException> {
            archive.toPath().unzipTarGz(targetDir)
        }
        assertFalse(
            outside.resolve("payload.txt").exists(),
            "Hardlink must not have been created outside targetDir via symlink"
        )
    }

    // CWE-59: hardlink target resolves outside targetDir.
    @Test
    fun `escaping hardlink is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val outsideFile = temporaryFolder.resolve("outside").createDirectories().resolve("hardlink_target.txt")
        outsideFile.writeText("outside")
        val hardlink = targetDir.resolve("kotlin-native/link.txt")
        val archive = createTarGz {
            directory("kotlin-native/")
            hardlink("kotlin-native/link.txt", "../outside/hardlink_target.txt")
        }

        assertThrows<TarExtractionSecurityException> {
            archive.toPath().unzipTarGz(targetDir)
        }
        assertFalse(hardlink.exists(), "Escaping hardlink was created: $hardlink")
    }

    // Control: a symlink that stays inside targetDir should still extract.
    @Test
    fun `intra-boundary symlink extracts successfully`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val archive = createTarGz {
            directory("kotlin-native/")
            directory("kotlin-native/inside/")
            symlink("kotlin-native/link", "inside")
            file("kotlin-native/link/payload.txt", "ok")
        }

        archive.toPath().unzipTarGz(targetDir)

        val extractedFile = targetDir.resolve("kotlin-native/inside/payload.txt")
        assertTrue(extractedFile.exists())
        assertEquals("ok", extractedFile.toFile().readText())
    }

    private fun createTarGz(build: TarArchiveOutputStream.() -> Unit): File {
        val tarFile = temporaryFolder.resolve("archive.tar.gz").toFile()
        TarArchiveOutputStream(
            GZIPOutputStream(
                BufferedOutputStream(
                    FileOutputStream(tarFile)
                )
            )
        ).use {
            it.apply(build)
        }
        return tarFile
    }

    private fun TarArchiveOutputStream.directory(name: String) {
        putArchiveEntry(TarArchiveEntry(name))
        closeArchiveEntry()
    }

    private fun TarArchiveOutputStream.file(name: String, contents: String) {
        val bytes = contents.toByteArray()
        val entry = TarArchiveEntry(name)
        entry.size = bytes.size.toLong()
        putArchiveEntry(entry)
        write(bytes)
        closeArchiveEntry()
    }

    private fun TarArchiveOutputStream.symlink(name: String, linkName: String) {
        val entry = TarArchiveEntry(name, TarConstants.LF_SYMLINK)
        entry.linkName = linkName
        putArchiveEntry(entry)
        closeArchiveEntry()
    }

    private fun TarArchiveOutputStream.hardlink(name: String, linkName: String) {
        val entry = TarArchiveEntry(name, TarConstants.LF_LINK)
        entry.linkName = linkName
        putArchiveEntry(entry)
        closeArchiveEntry()
    }
}
