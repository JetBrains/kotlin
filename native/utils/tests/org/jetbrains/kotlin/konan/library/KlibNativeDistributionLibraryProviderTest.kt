/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KlibNativeDistributionLibraryProviderTest {
    @TempDir
    private lateinit var tempDir: File

    @Test
    fun `Loading platform libs without occasional system files in the Native distro`() = loadPlatformLibs(useSystemFiles = false)

    @Test
    fun `Loading platform libs with occasional system files in the Native distro`() = loadPlatformLibs(useSystemFiles = true)

    private fun loadPlatformLibs(useSystemFiles: Boolean) {
        val result = KlibLoader {
            libraryProviders(
                KlibNativeDistributionLibraryProvider(emulateNativeDistribution(useSystemFiles = useSystemFiles)) {
                    withPlatformLibs(KonanTarget.MACOS_ARM64)
                }
            )
        }.load()
        result.reportLoadingProblemsIfAny { _, message -> fail(message) }
        assertEquals(3, result.librariesStdlibFirst.size)
    }

    private fun emulateNativeDistribution(useSystemFiles: Boolean = false): File {
        val distDir = tempDir.resolve("kotlin-native-dist")

        with(distDir.resolve("klib/platform/macos_arm64")) {
            mkdirs()
            for (shortName in listOf("posix", "foo", "bar")) {
                val fullName = "org.jetbrains.kotlin.native.platform.$shortName"
                with(resolve("$fullName/default")) {
                    mkdirs()
                    resolve("manifest").writeText(
                        """
                            abi_version=${KotlinAbiVersion.CURRENT}
                            builtins_platform=NATIVE
                            native_targets=macos_arm64
                            package=platform.$shortName
                            unique_name=$fullName
                        """.trimIndent()
                    )
                }
            }
            if (useSystemFiles) {
                for (fileName in listOf(".DS_Store", "Desktop.ini")) {
                    resolve("$fileName.dir").mkdirs()
                    resolve("$fileName.file").writeText("")
                }
            }
        }

        return distDir
    }
}
