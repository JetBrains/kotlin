/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.compileToLibrary
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.text.startsWith

class NativeKlibCliArgumentsTest : AbstractNativeSimpleTest() {
    @Test
    @DisplayName("Test custom -Xabi-version CLI argument (KT-74467)")
    fun testCustomAbiVersionCliArgument() {
        val sourcesDir = buildDir.resolve("sources").apply { createDirectory() }
        val sourceFile = sourcesDir.resolve("source.kt").apply { writeText("fun foo() = Unit") }

        val outputDir = buildDir.resolve("output").apply { createDirectory() }

        val correctVersions = arrayOf(
            "0.0.0", "255.255.255",
            "0.10.200", "10.200.0", "200.0.10",
            "2.2.0", "2.3.0"
        )
        for (version in correctVersions) {
            val klibDir = compileToLibrary(
                sourcesDir = sourceFile,
                outputDir = outputDir,
                freeCompilerArgs = TestCompilerArgs(
                    K2NativeCompilerArguments::customKlibAbiVersion.cliArgument + "=" + version,
                    K2NativeCompilerArguments::nopack.cliArgument,
                ),
                dependencies = emptyList(),
            ).guessKlibArtifactFile()

            val manifest = klibDir.resolve("default/manifest")
            val versionBumped = manifest.readLines()
                .find { it.startsWith("abi_version") }
                ?.split("=")
                ?.get(1)

            assertEquals(version, versionBumped)
        }

        val incorrectVersions = arrayOf(
            "0", "0.1", "0.1.", "0.1.2.", "..", "0 .1. 2",
            "00.001.0002", "-0.-0.-0", "256.256.256"
        )
        for (version in incorrectVersions) {
            try {
                compileToLibrary(
                    sourcesDir = sourceFile,
                    outputDir = outputDir,
                    freeCompilerArgs = TestCompilerArgs(
                        K2NativeCompilerArguments::customKlibAbiVersion.cliArgument + "=" + version,
                        K2NativeCompilerArguments::nopack.cliArgument,
                    ),
                    dependencies = emptyList(),
                )
                fail { "Compilation should fail" }
            } catch (cte: CompilationToolException) {
                assertTrue(cte.reason.contains("error: invalid ABI version")) { "Unexpected error message: ${cte.reason}" }
            }
        }
    }

    @Test
    @DisplayName("Test custom -Xmetadata-version CLI argument (KT-56062)")
    fun testCustomMetadataVersionCliArgument() {
        val dir = buildDir.resolve("dir").apply { createDirectory() }
        val sourceFile = dir.resolve("source.kt").apply { writeText("fun foo() = Unit") }

        val correctVersions = arrayOf(
            "0.0.0", "255.255.255",
            "1.4.1", "2.1.0", "2.2.0", "2.3.0"
        )
        for (version in correctVersions) {
            val klibDir = compileToLibrary(
                sourcesDir = sourceFile,
                outputDir = dir,
                freeCompilerArgs = TestCompilerArgs(
                    K2NativeCompilerArguments::metadataVersion.cliArgument + "=" + version,
                    K2NativeCompilerArguments::nopack.cliArgument,
                ),
                dependencies = emptyList(),
            ).guessKlibArtifactFile()

            val manifest = klibDir.resolve("default/manifest")
            val versionBumped = manifest.readLines()
                .find { it.startsWith("metadata_version") }
                ?.split("=")
                ?.get(1)

            assertEquals(version, versionBumped)
        }

        val incorrectVersions = arrayOf(
            "0.1.", "0.1.2.", "..", "0 .1. 2",
            // These test cases should be uncommented after fixing KT-76247
            // "0", "0.1", "0.1.2.3",
            // "00.001.0002", "-0.-0.-0", "256.256.256"
        )
        for (version in incorrectVersions) {
            try {
                compileToLibrary(
                    sourcesDir = sourceFile,
                    outputDir = dir,
                    freeCompilerArgs = TestCompilerArgs(
                        K2NativeCompilerArguments::metadataVersion.cliArgument + "=" + version,
                        K2NativeCompilerArguments::nopack.cliArgument,
                    ),
                    dependencies = emptyList(),
                )
                fail { "Compilation should fail" }
            } catch (cte: CompilationToolException) {
                assertTrue(cte.reason.contains("error: invalid metadata version")) { "Unexpected error message: ${cte.reason}" }
            }
        }
    }

    companion object {
        private fun TestCompilationArtifact.KLIB.guessKlibArtifactFile(): File {
            val klibFile = this.klibFile
            if (!klibFile.exists() && klibFile.extension == "klib") {
                val klibDir = klibFile.resolveSibling(klibFile.nameWithoutExtension)
                if (klibDir.exists()) return klibDir
            }
            return klibFile
        }
    }
}