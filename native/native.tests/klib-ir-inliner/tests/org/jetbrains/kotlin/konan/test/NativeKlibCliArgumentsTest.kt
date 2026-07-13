/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.asLibraryDependency
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.compileToLibrary
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.util.toMetadataVersion
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

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

    @Test
    @DisplayName("Test manifest metadata version (KT-74417)")
    fun testManifestMetadataVersion() {
        val dir = buildDir.resolve("dir").apply { createDirectory() }
        val sourceFile = dir.resolve("source.kt").apply { writeText("fun foo() = Unit") }
        val currentLanguageVersion = LanguageVersion.LATEST_STABLE
        val currentMetadataVersion = currentLanguageVersion.toMetadataVersion().toString()
        val testData = arrayOf(
            Pair(
                TestCompilerArgs("-language-version", currentLanguageVersion.versionString, K2NativeCompilerArguments::nopack.cliArgument),
                currentMetadataVersion,
            ),
            Pair(
                TestCompilerArgs(
                    "-language-version", currentLanguageVersion.versionString, K2NativeCompilerArguments::nopack.cliArgument,
                    K2NativeCompilerArguments::metadataVersion.cliArgument + "=2.3.0",
                ),
                "2.3.0",
            ),
        )

        for ([args, expectedVersion] in testData) {
            val klibDir = compileToLibrary(
                sourcesDir = sourceFile,
                outputDir = dir,
                freeCompilerArgs = args,
                dependencies = emptyList(),
            ).guessKlibArtifactFile()

            val manifest = klibDir.resolve("default/manifest")
            val metadataVersion = manifest.readLines()
                .find { it.startsWith("metadata_version") }
                ?.split("=")
                ?.get(1)

            assertEquals(expectedVersion, metadataVersion)
        }
    }

    @Test
    @DisplayName("Test compilation against dependencies with supported metadata versions (KT-55808)")
    fun testCompileAgainstDependencyWithSupportedMetadataVersions() {
        val currentLanguageVersionIndex = LanguageVersion.entries.indexOf(LanguageVersion.LATEST_STABLE)
        val supportedDependencyMetadataVersions = listOf(
            LanguageVersion.entries[currentLanguageVersionIndex - 2],
            LanguageVersion.entries[currentLanguageVersionIndex - 1],
            LanguageVersion.LATEST_STABLE,
            LanguageVersion.entries[currentLanguageVersionIndex + 1],
        ).map { it.toMetadataVersion().toString() }

        val dependencySourceDir = buildDir.resolve("lib1").apply { createDirectory() }
        dependencySourceDir.resolve("lib1.kt").writeText(
            """
            package lib1

            fun foo() = "Hello"
            """.trimIndent()
        )
        val usageSourceDir = buildDir.resolve("lib2").apply { createDirectory() }
        usageSourceDir.resolve("lib2.kt").writeText(
            """
            package lib2

            fun bar() = lib1.foo()
            """.trimIndent()
        )

        for (metadataVersion in supportedDependencyMetadataVersions) {
            val dependency = compileToLibrary(
                sourcesDir = dependencySourceDir,
                outputDir = buildDir,
                freeCompilerArgs = TestCompilerArgs(
                    K2NativeCompilerArguments::metadataVersion.cliArgument + "=" + metadataVersion,
                    CommonCompilerArguments::skipMetadataVersionCheck.cliArgument,
                ),
                dependencies = emptyList(),
            )

            compileToLibrary(
                sourcesDir = usageSourceDir,
                outputDir = buildDir,
                freeCompilerArgs = TestCompilerArgs.EMPTY,
                dependencies = listOf(dependency.asLibraryDependency()),
            )
        }
    }

    @Test
    @DisplayName("Test compilation against dependencies with unsupported metadata versions (KT-55808)")
    fun testCompileAgainstDependencyWithUnsupportedMetadataVersions() {
        val currentLanguageVersionIndex = LanguageVersion.entries.indexOf(LanguageVersion.LATEST_STABLE)
        val metadataVersion = LanguageVersion.entries[currentLanguageVersionIndex + 2].toMetadataVersion().toString()

        val dependencySourceDir = buildDir.resolve("lib1").apply { createDirectory() }
        dependencySourceDir.resolve("lib1.kt").writeText(
            """
            package lib1

            fun foo() = "Hello"
            """.trimIndent()
        )
        val dependency = compileToLibrary(
            sourcesDir = dependencySourceDir,
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs(
                K2NativeCompilerArguments::metadataVersion.cliArgument + "=" + metadataVersion,
                CommonCompilerArguments::skipMetadataVersionCheck.cliArgument,
            ),
            dependencies = emptyList(),
        )

        val usageSourceDir = buildDir.resolve("lib2").apply { createDirectory() }
        usageSourceDir.resolve("lib2.kt").writeText(
            """
            package lib2

            fun bar() = lib1.foo()
            """.trimIndent()
        )

        try {
            compileToLibrary(
                sourcesDir = usageSourceDir,
                outputDir = buildDir,
                freeCompilerArgs = TestCompilerArgs.EMPTY,
                dependencies = listOf(dependency.asLibraryDependency()),
            )
            fail { "Compilation should fail" }
        } catch (cte: CompilationToolException) {
            assertTrue(cte.reason.contains("compiled with an incompatible version of Kotlin"))
            assertTrue(cte.reason.contains("The actual metadata version is $metadataVersion"))
        }
    }

    @Test
    @DisplayName("Test -Xexport-kdoc argument")
    fun testExportKdocArgument() {
        val sourcesDir = buildDir.resolve("sources").apply { createDirectory() }

        val packageName = "test"

        sourcesDir.resolve("source.kt").apply {
            writeText(
                """
                    package $packageName
                    
                    /**
                     * I'm the main function!
                     */
                    fun main() {}
                """.trimIndent()
            )
        }

        val binariesDir = buildDir.resolve("binaries")

        val klibFile = compileToLibrary(
            sourcesDir = sourcesDir,
            outputDir = binariesDir,
            freeCompilerArgs = TestCompilerArgs("-Xexport-kdoc"),
            dependencies = emptyList(),
        ).guessKlibArtifactFile()

        assertTrue(klibFile.exists()) { "Klib file should exist" }

        val kotlinLibrary = KlibLoader { libraryPaths(klibFile) }.load().librariesStdlibFirst.single()
        val metadata = kotlinLibrary.metadata

        val packageFragmentName = kotlinLibrary.metadata.getPackageFragmentNames(packageName).single()
        val packageFragment = parsePackageFragment(metadata.getPackageFragment(packageName, packageFragmentName))
        val nameResolver = NameResolverImpl(packageFragment.strings, packageFragment.qualifiedNames)

        val mainFunctionProto = packageFragment.`package`.functionList.single()
        assertEquals("main", nameResolver.getName(mainFunctionProto.name).asString())

        // Raw representation with the starting asterisks!
        assertEquals(
            """
                /**
                 * I'm the main function!
                 */
            """.trimIndent(),
            mainFunctionProto.getExtensionOrNull(KlibMetadataProtoBuf.functionKdoc)
        )
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
