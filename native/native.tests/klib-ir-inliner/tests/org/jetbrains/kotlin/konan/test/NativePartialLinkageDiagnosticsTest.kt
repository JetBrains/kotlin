/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.asLibraryDependency
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.compileToExecutableInOneStage
import org.jetbrains.kotlin.konan.test.blackbox.compileToLibrary
import org.jetbrains.kotlin.konan.test.blackbox.generateTestCaseWithSingleModule
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UsePartialLinkage
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

abstract class NativePartialLinkageDiagnosticsTest : AbstractNativeSimpleTest() {
    fun prepareExecutable(): TestCompilationResult<out TestCompilationArtifact.Executable> {
        val lib11Source = buildDir.resolve("lib1.kt").apply {
            writeText(
                """
            |package lib1
            |
            |fun foo() = 42
            """.trimMargin()
            )
        }
        val lib1Output = buildDir.resolve("lib1").apply { createDirectory() }
        val lib11 = compileToLibrary(
            sourcesDir = lib11Source,
            outputDir = lib1Output,
            freeCompilerArgs = TestCompilerArgs(),
            dependencies = emptyList(),
        )

        // Create lib2 that depends on both functions from lib1
        val lib2Source = buildDir.resolve("lib2.kt").apply {
            writeText(
                """
            |package lib2
            |import lib1.foo
            |
            |fun bar() = foo()
            """.trimMargin()
            )
        }
        val lib2Output = buildDir.resolve("lib2").apply { createDirectory() }
        val lib2 = compileToLibrary(
            sourcesDir = lib2Source,
            outputDir = lib2Output,
            freeCompilerArgs = TestCompilerArgs(),
            dependencies = listOf(lib11.asLibraryDependency()),
        )

        val lib12Source = buildDir.resolve("lib1.kt").apply {
            writeText(
                """
            |package lib1
            |
            |//fun foo() = 42
            """.trimMargin()
            )
        }
        val lib12 = compileToLibrary(
            sourcesDir = lib12Source,
            outputDir = lib1Output,
            freeCompilerArgs = TestCompilerArgs(),
            dependencies = emptyList(),
        )

        val executableSource = buildDir.resolve("main.kt").apply {
            writeText(
                """
            |import lib2.bar
            |
            |fun main() = bar()
            """.trimMargin()
            )
        }

        val testCase = generateTestCaseWithSingleModule(
            executableSource,
            TestCompilerArgs()
        )
        return compileToExecutableInOneStage(
            testCase,
            lib12.asLibraryDependency(),
            lib2.asLibraryDependency()
        )
    }

    companion object {
        fun TestCompilationResult.ImmediateResult<*>.toolOutput(): String =
            (loggedData as LoggedData.CompilationToolCall).toolOutput
    }
}

@UsePartialLinkage(UsePartialLinkage.Mode.SILENT)
class NativePartialLinkageSilentDiagnosticsTest : NativePartialLinkageDiagnosticsTest() {
    @Test
    @DisplayName("Test partial linkage silent diagnostics")
    fun testPartialLinkageSilentDiagnostics() {
        val output = prepareExecutable().assertSuccess().toolOutput()
        assert(output.isEmpty())
    }
}

@UsePartialLinkage(UsePartialLinkage.Mode.INFO)
class NativePartialLinkageInfoDiagnosticsTest : NativePartialLinkageDiagnosticsTest() {
    @Test
    @DisplayName("Test partial linkage info diagnostics")
    fun testPartialLinkageInfoDiagnostics() {
        val output = prepareExecutable().assertSuccess().toolOutput()
        assert(output.contains("warning: <lib2>"))
    }
}

@UsePartialLinkage(UsePartialLinkage.Mode.WARNING)
class NativePartialLinkageWarningDiagnosticsTest : NativePartialLinkageDiagnosticsTest() {
    @Test
    @DisplayName("Test partial linkage warning diagnostics")
    fun testPartialLinkageWarningDiagnostics() {
        val output = prepareExecutable().assertSuccess().toolOutput()
        assert(output.contains("warning: <lib2>"))
    }
}

@UsePartialLinkage(UsePartialLinkage.Mode.ERROR)
class NativePartialLinkageErrorDiagnosticsTest : NativePartialLinkageDiagnosticsTest() {
    @Test
    @DisplayName("Test partial linkage error diagnostics")
    fun testPartialLinkageErrorDiagnostics() {
        val result = prepareExecutable()

        assertIs<TestCompilationResult.CompilationToolFailure>(result)
        assert(result.toolOutput().contains("error: <lib2>"))
    }
}