/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.utils.konanHome
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

public class CommonizeLibcurlTest {

    @get:Rule
    public val temporaryOutputDirectory: TemporaryFolder = TemporaryFolder()

    @Test
    public fun commonizeSuccessfully() {
        val libraries = File("testData/libcurl").walkTopDown().filter { it.isFile && it.extension == "klib" }.toSet()
        val commonizer = CliCommonizer(this::class.java.classLoader)

        commonizer.commonizeLibraries(
            konanHome = konanHome,
            inputLibraries = libraries,
            dependencyLibraries = emptySet<CommonizerDependency>() +
                    KonanDistribution(konanHome).platformLibsDir.resolve(LINUX_X64.name).listFiles().orEmpty()
                        .map { TargetedCommonizerDependency(LeafCommonizerTarget(LINUX_X64), it) }.toSet() +

                    KonanDistribution(konanHome).platformLibsDir.resolve(LINUX_ARM64.name).listFiles().orEmpty()
                        .map { TargetedCommonizerDependency(LeafCommonizerTarget(LINUX_ARM64), it) }
                        .toSet(),
            outputTargets = setOf(CommonizerTarget(LINUX_ARM64, LINUX_X64)),
            outputDirectory = temporaryOutputDirectory.root,
            logLevel = CommonizerLogLevel.Info
        )

        val commonOutputDirectory = temporaryOutputDirectory.root.resolve(CommonizerTarget(LINUX_X64, LINUX_ARM64).identityString)

        assertTrue(
            commonOutputDirectory.exists(),
            "Missing output directory for commonized x64&arm64 target"
        )

        fun assertContainsKnmFiles(file: File) {
            assertTrue(
                file.walkTopDown().any { it.extension == "knm" },
                "Expected directory ${file.name} to contain at least one knm file"
            )
        }

        assertContainsKnmFiles(commonOutputDirectory)
        assertContainsManifestWithContent(commonOutputDirectory, "native_targets=linux_arm64 linux_x64")
        assertContainsManifestWithContent(commonOutputDirectory, "commonizer_native_targets=linux_arm64 linux_x64")
        assertContainsManifestWithContent(
            commonOutputDirectory, "commonizer_target=${CommonizerTarget(LINUX_X64, LINUX_ARM64).identityString}"
        )
    }


    @Test
    public fun `commonizeSuccessfully with unsupported targets`() {
        val libraries = File("testData/libcurl").walkTopDown().filter { it.isFile && it.extension == "klib" }.toSet()
        val commonizer = CliCommonizer(this::class.java.classLoader)

        commonizer.commonizeLibraries(
            konanHome = konanHome,
            inputLibraries = libraries,
            dependencyLibraries = emptySet<CommonizerDependency>() +
                    KonanDistribution(konanHome).platformLibsDir.resolve(LINUX_X64.name).listFiles().orEmpty()
                        .map { TargetedCommonizerDependency(LeafCommonizerTarget(LINUX_X64), it) }.toSet() +

                    KonanDistribution(konanHome).platformLibsDir.resolve(LINUX_ARM64.name).listFiles().orEmpty()
                        .map { TargetedCommonizerDependency(LeafCommonizerTarget(LINUX_ARM64), it) }
                        .toSet(),
            outputTargets = setOf(CommonizerTarget(LINUX_ARM64, LINUX_X64, MACOS_X64)),
            outputDirectory = temporaryOutputDirectory.root
        )

        val commonOutputDirectory = temporaryOutputDirectory.root
            .resolve(CommonizerTarget(LINUX_X64, LINUX_ARM64, MACOS_X64).identityString)

        assertContainsManifestWithContent(commonOutputDirectory, "native_targets=linux_arm64 linux_x64")
        assertContainsManifestWithContent(commonOutputDirectory, "commonizer_native_targets=linux_arm64 linux_x64 macos_x64")
        assertContainsManifestWithContent(
            commonOutputDirectory, "commonizer_target=${CommonizerTarget(LINUX_X64, LINUX_ARM64, MACOS_X64).identityString}"
        )
    }
}

private fun assertContainsManifestWithContent(directory: File, content: String) {
    val manifest = directory.walkTopDown().firstOrNull { it.name == "manifest" }
        ?: fail("${directory.name} does not contain any manifest")

    assertTrue(
        content in manifest.readText(),
        "Expected manifest in ${directory.name} to contain $content\n${manifest.readText()}"
    )
}
