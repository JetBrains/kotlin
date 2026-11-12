/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.jetbrains.kotlin.gradle.targets.native.toolchain.NativeVersionValueSource
import org.jetbrains.kotlin.gradle.testing.WithTemporaryFolder
import org.jetbrains.kotlin.gradle.testing.newTempDirectory
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.FileSystemException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativeCompilerDownloaderTest : WithTemporaryFolder {

    @field:TempDir
    override lateinit var temporaryFolder: Path

    private fun createUnpackedDistribution(parent: Path, relativePath: String): File {
        val distribution = parent.resolve(relativePath).createDirectories()
        distribution.resolve("bin").createDirectory()
        distribution.resolve("bin").resolve("konanc").createFile().writeText(relativePath)
        return distribution.toFile()
    }

    @Test
    fun publishSucceedsWhenDestinationIsFree() {
        val konanDir = newTempDirectory()
        val source = createUnpackedDistribution(konanDir, "compiler-1/kotlin-native-prebuilt-arch-2.5.20")
        val destination = konanDir.resolve("kotlin-native-prebuilt-arch-2.5.20").toFile()

        val published = NativeCompilerDownloader.publishExtractedDistribution(source, destination)

        assertTrue(published)
        assertTrue("Distribution should be published to the destination") { destination.resolve("bin/konanc").isFile }
        assertFalse("Source should be gone after the move") { source.exists() }
    }

    @Test
    fun publishReplacesEmptyLeftoverDirectory() {
        val konanDir = newTempDirectory()
        val source = createUnpackedDistribution(konanDir, "compiler-1/kotlin-native-prebuilt-arch-2.5.20")
        val destination = konanDir.resolve("kotlin-native-prebuilt-arch-2.5.20").createDirectory().toFile()

        val published = NativeCompilerDownloader.publishExtractedDistribution(source, destination)

        assertTrue(published)
        assertTrue("Leftover empty directory should be replaced by the distribution") { destination.resolve("bin/konanc").isFile }
    }

    @Test
    fun publishToleratesAlreadyInstalledDestination() {
        val konanDir = newTempDirectory()
        val source = createUnpackedDistribution(konanDir, "compiler-1/kotlin-native-prebuilt-arch-2.5.20")
        val winner = createUnpackedDistribution(konanDir, "kotlin-native-prebuilt-arch-2.5.20")

        val published = NativeCompilerDownloader.publishExtractedDistribution(source, winner)

        assertFalse(published, "Move losing to a concurrent installation should be tolerated")
        assertTrue(
            "Winner's distribution should stay intact (KT-86251)",
        ) { winner.resolve("bin/konanc").readText().endsWith("kotlin-native-prebuilt-arch-2.5.20") }
        assertTrue("Source should be left for the caller to clean up") { source.exists() }
    }

    @Test
    fun publishToleratesDestinationAppearingDuringMove() {
        val konanDir = newTempDirectory()
        val source = createUnpackedDistribution(konanDir, "compiler-1/kotlin-native-prebuilt-arch-2.5.20")
        val destination = createUnpackedDistribution(konanDir, "kotlin-native-prebuilt-arch-2.5.20")

        val published = NativeCompilerDownloader.publishExtractedDistribution(source, destination) {
            // when the destination shows up inside 'Files.move' between its own existence check and the rename,
            // 'rename(2)' fails with ENOTEMPTY and the JDK reports that as a plain 'FileSystemException' with the
            // reason 'Directory not empty' (see the CI failure: source -> destination: Directory not empty)
            throw FileSystemException(source.absolutePath, destination.absolutePath, "Directory not empty")
        }

        assertFalse(published, "Move losing the race inside 'Files.move' should be tolerated")
    }

    @Test
    fun publishFailsWhenNothingWasInstalled() {
        val konanDir = newTempDirectory()
        val source = createUnpackedDistribution(konanDir, "compiler-1/kotlin-native-prebuilt-arch-2.5.20")
        val destination = konanDir.resolve("kotlin-native-prebuilt-arch-2.5.20").toFile()

        assertFailsWith<FileSystemException>("A failed move without any distribution in place should fail the build") {
            NativeCompilerDownloader.publishExtractedDistribution(source, destination) {
                throw FileSystemException(source.absolutePath, destination.absolutePath, "Directory not empty")
            }
        }
    }

    @Test
    fun provisionedDistributionIsRecognizedByTheMarkerFile() {
        val konanDir = newTempDirectory()
        val distribution = createUnpackedDistribution(konanDir, "kotlin-native-prebuilt-arch-2.5.20")

        assertFalse(NativeCompilerDownloader.isDistributionProvisioned(distribution))
        NativeVersionValueSource.markAsProvisioned(distribution)
        assertTrue(NativeCompilerDownloader.isDistributionProvisioned(distribution))
    }
}
