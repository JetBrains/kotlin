/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.library.components.bitcode
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.library.isHeader
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.junit.jupiter.api.Assertions.assertFalse
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@Tag("cinterop")
class CInteropHeaderModeValidationTest : AbstractNativeSimpleTest() {
    private lateinit var defFile: File
    private lateinit var headerFile: File
    private lateinit var dependentKotlinDir: File

    @BeforeEach
    fun setUp() {
        // 1. Create header with a valid function declaration, but define a macro that overrides
        // the function call with syntactically invalid C code.
        // This parses successfully in cinterop (Kotlin signature is generated), but when compiling
        // the wrapper C bridge function, the macro expands and causes a C compilation failure.
        val includeDir = buildDir.resolve("include").apply { mkdirs() }
        headerFile = includeDir.resolve("invalidBridge.h").apply {
            writeText(
                """
                    void testInvalidBridge(int x);
                    #define testInvalidBridge(x) (x = "error")
                """.trimIndent()
            )
        }
        
        defFile = buildDir.resolve("library.def").apply {
            writeText(
                """
                    language = C
                    headers = invalidBridge.h
                """.trimIndent()
            )
        }

        // 2. Create a dependent Kotlin file that calls the function
        dependentKotlinDir = buildDir.resolve("dependentKotlin").apply { mkdirs() }
        dependentKotlinDir.resolve("main.kt").apply {
            writeText(
                """
                    @file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                    import library.*
                    
                    fun main() {
                        // This call depends on the invalid bridge function
                        testInvalidBridge(0) 
                    }
                """.trimIndent()
            )
        }
    }

    @Test
    fun testHeaderModeCompilationAvoidancePipeline() {
        val includeArg = listOf("-compiler-option", "-I${headerFile.parentFile.canonicalPath}")

        // 1. Compile CInterop in Header Mode
        val headerKlib = cinteropToLibrary(
            defFile,
            buildDir.resolve("headerModeOut").apply { mkdirs() },
            TestCInteropArgs(includeArg + "-Xheader-mode")
        ).assertSuccess().resultingArtifact

        val headerLibrary = KlibLoader { libraryPaths(headerKlib.klibFile.path) }.load().librariesStdlibFirst.single()
        assertTrue(headerLibrary.isHeader) { "Expected header klib to have header=true in manifest" }

        val bitcodeFiles = headerLibrary.bitcode(targets.testTarget)?.bitcodeFilePaths ?: emptyList()
        assertTrue(bitcodeFiles.isEmpty()) { "Expected no bitcode files in header klib, got: $bitcodeFiles" }

        // 2. Compile dependent Kotlin code using the Header-Mode KLIB (Succeeds)
        val dependentKlibSuccess = compileToLibrary(
            dependentKotlinDir,
            buildDir.resolve("dependentHeaderSuccess").apply { mkdirs() },
            freeCompilerArgs = TestCompilerArgs.EMPTY,
            dependencies = listOf(headerKlib.asLibraryDependency())
        )
        assertTrue(dependentKlibSuccess.klibFile.exists())

        // Full-mode cinterop compilation and binary linking require running on the matching host platform
        if (!targets.areDifferentTargets()) {
            // 3. Compile cinterop in full mode
            val fullKlib = cinteropToLibrary(
                defFile,
                buildDir.resolve("fullModeOut").apply { mkdirs() },
                TestCInteropArgs(includeArg)
            ).assertSuccess().resultingArtifact

            val fullLibrary = KlibLoader { libraryPaths(fullKlib.klibFile.path) }.load().librariesStdlibFirst.single()
            assertFalse(fullLibrary.isHeader) { "Expected full klib to not have header=true in manifest" }

            // 4. Link the pre-compiled dependent KLIB with the Full-Mode KLIB (fails linking)
            val linkTestCase = generateTestCaseWithSingleModule(null, TestCompilerArgs.EMPTY)
            val result = compileToExecutableInOneStage(
                linkTestCase,
                dependentKlibSuccess.asLibraryDependency(),
                fullKlib.asLibraryDependency()
            )

            // The linking step must fail because fullKlib is missing testInvalidBridge's bridge implementation
            assertTrue(result is TestCompilationResult.CompilationToolFailure) {
                "Expected link compilation to fail, but it succeeded! Result: $result"
            }
        }
    }

    @Test
    fun testHeaderModeCrossCompilation() {
        // Cross-targets supported on this host (e.g. Linux host supports linux_arm64, android_arm64, android_x64, mingw_x64)
        val crossTargets = listOf(
            KonanTarget.LINUX_ARM64,
            KonanTarget.ANDROID_ARM64,
            KonanTarget.ANDROID_X64,
            KonanTarget.MINGW_X64,
        )

        val includeArg = listOf("-compiler-option", "-I${headerFile.parentFile.canonicalPath}")

        for (target in crossTargets) {
            val targetSettings = object : Settings(
                testRunSettings,
                listOf(KotlinNativeTargets(target, targets.hostTarget))
            ) {}

            val targetBuildDir = buildDir.resolve("cross_${target.name}").apply { mkdirs() }

            // 1. Cross-compile CInterop in Header Mode
            val testCase = generateCInteropTestCaseFromSingleDefFile(defFile, TestCInteropArgs(includeArg + "-Xheader-mode"))
            val headerKlib = CInteropCompilation(
                settings = targetSettings,
                freeCompilerArgs = TestCInteropArgs(includeArg + "-Xheader-mode"),
                defFile = testCase.modules.single().files.single().location,
                dependencies = emptyList(),
                expectedArtifact = getLibraryArtifact(testCase, targetBuildDir.resolve("headerModeOut").apply { mkdirs() }, true),
                noDefaultLibs = true
            ).result.assertSuccess().resultingArtifact

            val headerLibrary = KlibLoader { libraryPaths(headerKlib.klibFile.path) }.load().librariesStdlibFirst.single()
            val bitcodeFiles = headerLibrary.bitcode(target)?.bitcodeFilePaths ?: emptyList()
            assertTrue(bitcodeFiles.isEmpty()) { "Expected no bitcode in header klib for target $target, got: $bitcodeFiles" }

            // 2. Cross-compile dependent Kotlin code against the Header-Mode KLIB
            val dependentTestCase = generateTestCaseWithSingleModule(dependentKotlinDir, TestCompilerArgs.EMPTY)
            val dependentKlibSuccess = LibraryCompilation(
                settings = targetSettings,
                freeCompilerArgs = TestCompilerArgs.EMPTY,
                sourceModules = dependentTestCase.modules,
                dependencies = listOf(headerKlib.asLibraryDependency()),
                expectedArtifact = getLibraryArtifact(dependentTestCase, targetBuildDir.resolve("dependentHeaderSuccess").apply { mkdirs() }, true)
            ).result.assertSuccess().resultingArtifact

            assertTrue(dependentKlibSuccess.klibFile.exists()) { "Expected dependent KLIB to be generated for target $target" }
        }
    }
}
