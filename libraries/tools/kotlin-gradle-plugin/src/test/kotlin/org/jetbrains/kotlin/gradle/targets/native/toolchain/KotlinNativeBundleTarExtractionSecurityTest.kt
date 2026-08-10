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
import java.nio.file.Paths
import java.util.zip.GZIPOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
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

    // CWE-59: writing a file *through* an escaping symlink is the actual exploit.
    // The symlink is planted first, then a later entry routes through it.
    @Test
    fun `write-through escaping symlink is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val outsideDir = temporaryFolder.resolve("outside").createDirectories()
        val archive = createTarGz {
            directory("kotlin-native/")
            // Plant an escaping symlink: kotlin-native/link -> ../../outside (outside targetDir)
            symlink("kotlin-native/link", "../../outside")
            // Then try to write a file through it
            file("kotlin-native/link/evil.txt", "evil")
        }

        assertThrows<TarExtractionSecurityException> {
            archive.toPath().unzipTarGz(targetDir)
        }
        assertFalse(outsideDir.resolve("evil.txt").exists(), "File written outside via symlink: $outsideDir/evil.txt")
    }

    // CWE-59: writing through a chain of symlinks (b -> a -> outside) must be rejected.
    // b's own target ("a") stays inside the target dir, so a purely lexical check misses it.
    @Test
    fun `chained escaping symlink write-through is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val outsideDir = temporaryFolder.resolve("outside").createDirectories()
        val archive = createTarGz {
            directory("kotlin-native/")
            // `a` escapes the target directory; `b` points at `a` (lexically inside the target).
            symlink("kotlin-native/a", "../../outside")
            symlink("kotlin-native/b", "a")
            // Writing through `b` routes b -> a -> outside and must be rejected.
            file("kotlin-native/b/evil.txt", "evil")
        }

        assertThrows<TarExtractionSecurityException> {
            archive.toPath().unzipTarGz(targetDir)
        }
        assertFalse(
            outsideDir.resolve("evil.txt").exists(),
            "File written outside via chained symlink: $outsideDir/evil.txt"
        )
    }

    // KT-87190 regression: an outward-pointing symlink (no write-through) must extract successfully.
    // Xcode addon bundles legitimately contain symlinks into SharedFrameworks outside the archive root.
    @Test
    fun `outward-pointing symlink without write-through extracts successfully`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val archive = createTarGz {
            directory("kotlin-native/")
            directory("kotlin-native/bin/")
            // Mimics xcode-addon: bin/altool -> ../../../SharedFrameworks/ContentDelivery.framework/...
            symlink("kotlin-native/bin/altool", "../../../SharedFrameworks/ContentDelivery.framework/Resources/altoolShim")
        }

        // Must not throw — the symlink is inert (no file is written through it)
        archive.toPath().unzipTarGz(targetDir)

        val symlinkPath = targetDir.resolve("kotlin-native/bin/altool")
        assertTrue(
            Files.exists(symlinkPath, LinkOption.NOFOLLOW_LINKS),
            "Outward symlink should be created: $symlinkPath"
        )
        assertEquals(
            Paths.get("../../../SharedFrameworks/ContentDelivery.framework/Resources/altoolShim"),
            Files.readSymbolicLink(symlinkPath)
        )
    }

    // CWE-59: hardlink target resolves outside targetDir.
    @Test
    fun `escaping hardlink is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        temporaryFolder.resolve("outside").createDirectories().resolve("hardlink_target.txt").writeText("outside")
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

    // Control: a symlink that stays inside targetDir should still extract and be usable.
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
        assertEquals("ok", extractedFile.readText())
    }

    // CWE-59: a hardlink target that lands under an escaping symlink declared *after* the hardlink
    // entry must still be rejected (the symlink set is only complete once all entries are read).
    @Test
    fun `hardlink target under a later escaping symlink is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        temporaryFolder.resolve("outside").createDirectories().resolve("real.txt").writeText("outside")
        val hardlink = targetDir.resolve("kotlin-native/hl.txt")
        val archive = createTarGz {
            directory("kotlin-native/")
            // Hardlink registered first; its target is lexically inside the root.
            hardlink("kotlin-native/hl.txt", "kotlin-native/sub/real.txt")
            // Escaping symlink declared afterwards routes kotlin-native/sub outside the root.
            symlink("kotlin-native/sub", "../../outside")
        }

        assertThrows<TarExtractionSecurityException> {
            archive.toPath().unzipTarGz(targetDir)
        }
        assertFalse(hardlink.exists(), "Escaping hardlink was created: $hardlink")
    }

    // CWE-59: a regular file entry must overwrite a symlink already present at its path, not follow it.
    @Test
    fun `regular file overwrites a pre-existing symlink instead of writing through it`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val outsideTarget = temporaryFolder.resolve("outside").createDirectories().resolve("victim.txt").also {
            it.writeText("original")
        }
        // Pre-seed the destination with an escaping symlink at the path the archive will write to.
        val planted = targetDir.resolve("kotlin-native").createDirectories().resolve("file.txt")
        Files.createSymbolicLink(planted, outsideTarget)

        val archive = createTarGz {
            directory("kotlin-native/")
            file("kotlin-native/file.txt", "fresh")
        }

        archive.toPath().unzipTarGz(targetDir)

        assertEquals("fresh", planted.readText(), "Extracted file should hold the archive content")
        assertFalse(Files.isSymbolicLink(planted), "Symlink should have been replaced by a real file")
        assertEquals("original", outsideTarget.readText(), "Outside file must not be written through the symlink")
    }

    // A dangling symlink ancestor can't be resolved to a real path; writing beneath it must be refused.
    @Test
    fun `write beneath a dangling symlink is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val archive = createTarGz {
            directory("kotlin-native/")
            symlink("kotlin-native/lib", "no-such-dir")
            file("kotlin-native/lib/x.txt", "payload")
        }

        assertThrows<TarExtractionSecurityException> {
            archive.toPath().unzipTarGz(targetDir)
        }
        assertFalse(targetDir.resolve("kotlin-native/no-such-dir/x.txt").exists())
    }

    // A file entry colliding with a non-empty directory must fail loudly, not delete extracted content.
    @Test
    fun `file entry over a non-empty directory is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val archive = createTarGz {
            directory("kotlin-native/")
            directory("kotlin-native/d/")
            file("kotlin-native/d/inner.txt", "keep me")
            file("kotlin-native/d", "clobber")
        }

        assertThrows<TarExtractionSecurityException> {
            archive.toPath().unzipTarGz(targetDir)
        }
        assertEquals("keep me", targetDir.resolve("kotlin-native/d/inner.txt").readText())
    }

    // Extracting the same archive twice must succeed (interrupted-run recovery); the
    // unlink-before-create in every branch is what makes symlink/hardlink entries re-extractable.
    @Test
    fun `re-extraction over an existing tree succeeds`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val archive = createTarGz {
            directory("kotlin-native/")
            file("kotlin-native/real.txt", "content")
            symlink("kotlin-native/link", "real.txt")
            hardlink("kotlin-native/hard.txt", "kotlin-native/real.txt")
        }

        archive.toPath().unzipTarGz(targetDir)
        archive.toPath().unzipTarGz(targetDir)

        assertEquals("content", targetDir.resolve("kotlin-native/real.txt").readText())
        assertEquals("content", targetDir.resolve("kotlin-native/link").readText())
        assertEquals("content", targetDir.resolve("kotlin-native/hard.txt").readText())
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
