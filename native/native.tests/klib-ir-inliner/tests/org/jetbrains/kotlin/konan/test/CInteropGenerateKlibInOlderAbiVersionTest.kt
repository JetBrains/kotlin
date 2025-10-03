/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.cinteropToLibrary
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@Tag("klib")
@Suppress("TestFunctionName")
class CInteropGenerateKlibInOlderAbiVersionTest : AbstractNativeSimpleTest() {
    private lateinit var outputDir: File
    private lateinit var defFile: File

    @BeforeEach
    fun setUp() {
        outputDir = buildDir.resolve("output").apply { mkdirs() }

        defFile = buildDir.resolve("library.def").apply {
            writeText(
                """
                    language = C
                    ---
                    void foo(void);
                """.trimIndent()
            )
        }
    }

    @Test
    fun abiCompatibilityLevelParsing() {
        class TestData(val abiCompatibilityLevel: String, val isSuccessExpected: Boolean)

        fun Good(abiCompatibilityLevel: String) = TestData(abiCompatibilityLevel, isSuccessExpected = true)
        fun Bad(abiCompatibilityLevel: String) = TestData(abiCompatibilityLevel, isSuccessExpected = false)

        listOf(
            Bad(" "),
            Bad("x"),
            Bad("2"),
            Good("2.2"),
            Bad("2.2.1"),
            Bad("2.2-Beta1"),
            Good("2.3"),
            Bad("2.4"),
        ).forEach { testData ->
            val cinteropArgs = listOf(
                "-Xklib-abi-compatibility-level", testData.abiCompatibilityLevel,
            )

            val cinteropResult = cinteropToLibrary(
                defFile = defFile,
                outputDir = outputDir,
                freeCompilerArgs = TestCompilerArgs(compilerArgs = emptyList(), cinteropArgs = cinteropArgs)
            )

            when (cinteropResult) {
                is TestCompilationResult.Success -> assertTrue(testData.isSuccessExpected) {
                    "Unexpected success with invalid C-interop arguments: $cinteropArgs"
                }

                is TestCompilationResult.CompilationToolFailure -> assertTrue(
                    !testData.isSuccessExpected &&
                            "Option Xklib-abi-compatibility-level is expected to be" in cinteropResult.loggedData.toolOutput
                ) {
                    "Unexpected failure with valid C-interop arguments: $cinteropArgs\n$cinteropResult"
                }

                else -> fail("Unexpected failure: $cinteropArgs\n$cinteropResult")
            }
        }
    }

    @Test
    fun oldAbiCompatibilityLevelCanBeUsedOnlyWithCCallModeIndirect() {
        class TestData(val abiCompatibilityLevel: String, val cCallMode: String, val isSuccessExpected: Boolean)

        fun Good(abiCompatibilityLevel: String, cCallMode: String) = TestData(abiCompatibilityLevel, cCallMode, isSuccessExpected = true)
        fun Bad(abiCompatibilityLevel: String, cCallMode: String) = TestData(abiCompatibilityLevel, cCallMode, isSuccessExpected = false)

        listOf(
            Good("2.2", "INDIRECT"),
            Bad("2.2", "DIRECT"),
            Bad("2.2", "BOTH"),
            Good("2.3", "INDIRECT"),
            Good("2.3", "DIRECT"),
            Good("2.3", "BOTH"),
        ).forEach { testData ->
            val cinteropArgs = listOf(
                "-Xklib-abi-compatibility-level", testData.abiCompatibilityLevel,
                "-Xccall-mode", testData.cCallMode,
            )

            val cinteropResult = cinteropToLibrary(
                defFile = defFile,
                outputDir = outputDir,
                freeCompilerArgs = TestCompilerArgs(compilerArgs = emptyList(), cinteropArgs = cinteropArgs)
            )

            when (cinteropResult) {
                is TestCompilationResult.Success -> assertTrue(testData.isSuccessExpected) {
                    "Unexpected success with invalid C-interop arguments: $cinteropArgs"
                }

                is TestCompilationResult.CompilationToolFailure -> assertTrue(
                    !testData.isSuccessExpected &&
                            "-Xccall-mode ${testData.cCallMode.lowercase()} is not supported in combination with -Xklib-abi-compatibility-level ${testData.abiCompatibilityLevel}" in cinteropResult.loggedData.toolOutput
                ) {
                    "Unexpected failure with valid C-interop arguments: $cinteropArgs\n$cinteropResult"
                }

                else -> fail("Unexpected failure: $cinteropArgs\n$cinteropResult")
            }
        }
    }

    @Test
    fun expectedAbiAndMetadataVersionsWrittenToKlibs() {
        class TestData(val abiCompatibilityLevel: String, val expectedAbiVersion: String, val expectedMetadataVersion: String)

        listOf(
            TestData(abiCompatibilityLevel = "2.2", expectedAbiVersion = "2.2.0", expectedMetadataVersion = "1.4.1"),
            TestData(abiCompatibilityLevel = "2.3", expectedAbiVersion = "2.3.0", expectedMetadataVersion = "2.3.0"),
        ).forEach { testData ->
            val cinteropArgs = listOf(
                "-Xklib-abi-compatibility-level", testData.abiCompatibilityLevel,
            )

            val cinteropResult = cinteropToLibrary(
                defFile = defFile,
                outputDir = outputDir,
                freeCompilerArgs = TestCompilerArgs(compilerArgs = emptyList(), cinteropArgs = cinteropArgs)
            )

            when (cinteropResult) {
                is TestCompilationResult.Success -> {
                    val loadedKlibs = KlibLoader { libraryPaths(cinteropResult.resultingArtifact.path) }.load()
                    assertFalse(loadedKlibs.hasProblems)
                    assertEquals(1, loadedKlibs.librariesStdlibFirst.size)

                    val library = loadedKlibs.librariesStdlibFirst.first()

                    assertEquals(testData.expectedAbiVersion, library.versions.abiVersion?.toString()) {
                        "Unexpected ABI version for ${library.libraryName}: ${library.versions.abiVersion} instead of ${testData.expectedAbiVersion}"
                    }

                    assertEquals(testData.expectedMetadataVersion, library.versions.metadataVersion?.toString()) {
                        "Unexpected metadata version for ${library.libraryName}: ${library.versions.metadataVersion} instead of ${testData.expectedMetadataVersion}"
                    }
                }

                else -> fail("Unexpected failure: $cinteropArgs\n$cinteropResult")
            }
        }
    }
}
