/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.api

import org.jetbrains.kotlin.commonizer.api.utils.konanHome
import org.jetbrains.kotlin.konan.target.KonanTarget.LINUX_X64
import org.jetbrains.kotlin.konan.target.KonanTarget.MACOS_X64
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

        commonizer(
            konanHome = konanHome,
            targetLibraries = libraries,
            dependencyLibraries = emptySet(),
            outputHierarchy = CommonizerTarget(MACOS_X64, LINUX_X64),
            outputDirectory = temporaryOutputDirectory.root
        )

        val linuxOutputDirectory = temporaryOutputDirectory.root.resolve(CommonizerTarget(LINUX_X64).identityString)
        val macosOutputDirectory = temporaryOutputDirectory.root.resolve(CommonizerTarget(MACOS_X64).identityString)
        val commonOutputDirectory = temporaryOutputDirectory.root.resolve(CommonizerTarget(LINUX_X64, MACOS_X64).identityString)


        assertTrue(
            linuxOutputDirectory.exists(),
            "Missing output directory for Linux target"
        )

        assertTrue(
            macosOutputDirectory.exists(),
            "Missing output directory for Macos target"
        )

        assertTrue(
            commonOutputDirectory.exists(),
            "Missing output directory for commonized Linux&Macos target"
        )

        fun assertContainsKnmFiles(file: File) {
            assertTrue(
                file.walkTopDown().any { it.extension == "knm" },
                "Expected directory ${file.name} to contain at least one knm file"
            )
        }

        assertContainsKnmFiles(linuxOutputDirectory)
        assertContainsKnmFiles(macosOutputDirectory)
        assertContainsKnmFiles(commonOutputDirectory)

        fun assertContainsManifestWithContent(directory: File, content: String) {
            val manifest = directory.walkTopDown().firstOrNull { it.name == "manifest" }
                ?: fail("${directory.name} does not contain any manifest")

            assertTrue(
                content in manifest.readText(),
                "Expected manifest in ${directory.name} to contain $content\n${manifest.readText()}"
            )
        }

        assertContainsManifestWithContent(linuxOutputDirectory, "native_targets=linux_x64")
        assertContainsManifestWithContent(macosOutputDirectory, "native_targets=macos_x64")
        assertContainsManifestWithContent(commonOutputDirectory, "native_targets=macos_x64 linux_x64")
    }
}
