/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.utils.konanHome
import org.jetbrains.kotlin.konan.target.KonanTarget.LINUX_ARM64
import org.jetbrains.kotlin.konan.target.KonanTarget.LINUX_X64
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class CommonizeLibcurlTest {

    @get:Rule
    val temporaryOutputDirectory = TemporaryFolder()

    @Test
    fun commonizeSuccessfully() {
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
            outputCommonizerTarget = CommonizerTarget(LINUX_ARM64, LINUX_X64),
            outputDirectory = temporaryOutputDirectory.root
        )

        val x64OutputDirectory = temporaryOutputDirectory.root.resolve(CommonizerTarget(LINUX_X64).identityString)
        val arm64OutputDirectory = temporaryOutputDirectory.root.resolve(CommonizerTarget(LINUX_ARM64).identityString)
        val commonOutputDirectory = temporaryOutputDirectory.root.resolve(CommonizerTarget(LINUX_X64, LINUX_ARM64).identityString)

        assertTrue(
            x64OutputDirectory.exists(),
            "Missing output directory for x64 target"
        )

        assertTrue(
            arm64OutputDirectory.exists(),
            "Missing output directory for arm64 target"
        )

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

        assertContainsKnmFiles(x64OutputDirectory)
        assertContainsKnmFiles(arm64OutputDirectory)
        assertContainsKnmFiles(commonOutputDirectory)

        fun assertContainsManifestWithContent(directory: File, content: String) {
            val manifest = directory.walkTopDown().firstOrNull { it.name == "manifest" }
                ?: fail("${directory.name} does not contain any manifest")

            assertTrue(
                content in manifest.readText(),
                "Expected manifest in ${directory.name} to contain $content\n${manifest.readText()}"
            )
        }

        assertContainsManifestWithContent(x64OutputDirectory, "native_targets=linux_x64")
        assertContainsManifestWithContent(arm64OutputDirectory, "native_targets=linux_arm64")
        assertContainsManifestWithContent(commonOutputDirectory, "native_targets=linux_x64 linux_arm64")
    }
}
