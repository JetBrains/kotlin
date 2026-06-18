/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
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

        // 2. Compile dependent Kotlin code using the Header-Mode KLIB (Succeeds)
        val dependentKlibSuccess = compileToLibrary(
            dependentKotlinDir,
            buildDir.resolve("dependentHeaderSuccess").apply { mkdirs() },
            freeCompilerArgs = TestCompilerArgs.EMPTY,
            dependencies = listOf(headerKlib.asLibraryDependency())
        )
        assertTrue(dependentKlibSuccess.klibFile.exists())

        // 3. Compile CInterop in Full Mode
        val fullKlib = cinteropToLibrary(
            defFile,
            buildDir.resolve("fullModeOut").apply { mkdirs() },
            TestCInteropArgs(includeArg)
        ).assertSuccess().resultingArtifact

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

    @Test
    fun testDetailedDeprecationMessage() {
        val includeArg = listOf("-compiler-option", "-I${headerFile.parentFile.canonicalPath}")

        // 1. Compile CInterop in Full Mode
        val fullKlib = cinteropToLibrary(
            defFile,
            buildDir.resolve("fullModeOutForDeprecation").apply { mkdirs() },
            TestCInteropArgs(includeArg)
        ).assertSuccess().resultingArtifact

        // 2. Compile dependent Kotlin code directly against the Full-Mode KLIB (fails compilation)
        val testCase = generateTestCaseWithSingleModule(dependentKotlinDir, TestCompilerArgs.EMPTY)
        val compilation = LibraryCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            dependencies = listOf(fullKlib.asLibraryDependency()),
            expectedArtifact = getLibraryArtifact(testCase, buildDir.resolve("dependentFullFailure").apply { mkdirs() })
        )
        val result = compilation.result

        // Verify that compile fails with our new descriptive error message
        assertTrue(result is TestCompilationResult.CompilationToolFailure) {
            "Expected CompilationToolFailure but got: $result. Details: " +
            when (result) {
                is TestCompilationResult.UnexpectedFailure -> "UnexpectedFailure LoggedData:\n${result.loggedData}"
                is TestCompilationResult.Success -> "Success Artifact: ${result.resultingArtifact}\nLoggedData:\n${result.loggedData}"
                is TestCompilationResult.DependencyFailures -> "DependencyFailures: ${result.causes}"
                else -> ""
            }
        }
        val errorMessage = (result as TestCompilationResult.CompilationToolFailure).loggedData.toString()
        assertTrue(errorMessage.contains("Unable to import this declaration. C bridge compilation failed.")) {
            "First assertion failed. Actual error message:\n$errorMessage"
        }
        assertTrue(errorMessage.contains("This declaration might be visible in the IDE/header-mode but cannot be compiled into the final binary.")) {
            "Second assertion failed. Actual error message:\n$errorMessage"
        }
    }
}
