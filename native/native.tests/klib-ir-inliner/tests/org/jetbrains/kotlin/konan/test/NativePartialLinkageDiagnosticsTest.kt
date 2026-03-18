/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.konan.test.blackbox.*
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UsePartialLinkage
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

abstract class NativePartialLinkageDiagnosticsTest : AbstractNativeSimpleTest() {
    fun prepareExecutable(): TestCompilationResult<out TestCompilationArtifact.Executable> {
        val lib11Source = buildDir.resolve("lib1.kt").apply { writeText(lib11SourceText) }
        val lib1Output = buildDir.resolve("lib1").apply { createDirectory() }
        val lib11 = compileToLibrary(
            sourcesDir = lib11Source,
            outputDir = lib1Output,
            freeCompilerArgs = TestCompilerArgs(),
            dependencies = emptyList(),
        )

        // Create lib2 that depends on both functions from lib1
        val lib2Source = buildDir.resolve("lib2.kt").apply { writeText(lib2SourceText) }
        val lib2Output = buildDir.resolve("lib2").apply { createDirectory() }
        val lib2 = compileToLibrary(
            sourcesDir = lib2Source,
            outputDir = lib2Output,
            freeCompilerArgs = TestCompilerArgs(),
            dependencies = listOf(lib11.asLibraryDependency()),
        )

        val lib12Source = buildDir.resolve("lib1.kt").apply { writeText(lib12SourceText) }
        val lib12 = compileToLibrary(
            sourcesDir = lib12Source,
            outputDir = lib1Output,
            freeCompilerArgs = TestCompilerArgs(),
            dependencies = emptyList(),
        )

        val executableSource = buildDir.resolve("main.kt").apply { writeText(mainSourceText) }
        val testCase = generateTestCaseWithSingleModule(
            executableSource,
            TestCompilerArgs("-Xklib-ir-inliner=full"),
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

        fun String.assertDiagnostics(
            shouldReportMinorSignificance: Boolean,
            shouldReportMajorSignificance: Boolean,
            shouldReportAsError: Boolean = false,
        ) {
            val severityForMinor = if (shouldReportAsError) "error" else "info"
            val severityForMajor = if (shouldReportAsError) "error" else "warning"

            // Minor issues
            assertEquals(shouldReportMinorSignificance, hasDiagnostic(memberOfUnlinkedClassesDiagnosticRegex(severityForMinor)))
            assertEquals(shouldReportMinorSignificance, hasDiagnostic(unusableAnnotationConstructorDiagnosticRegex(severityForMinor)))
            // Due to early abortion of compilation, this diagnostic is not reported for error severity.
            listOf("property" to "qux", "function" to "foo", "function" to "extensionFoo").forEach { (kind, name) ->
                assertEquals(
                    shouldReportMinorSignificance && !shouldReportAsError,
                    hasDiagnostic(reflectionTargetSymbolDiagnosticRegex(severityForMinor, kind, name))
                )
            }
            listOf("Removed", "Changed", "NoDefault").forEach {
                assertEquals(shouldReportMinorSignificance, hasDiagnostic(unusableAnnotationDiagnosticRegex(severityForMinor, it)))
            }
            listOf("Removed", "Changed").forEach {
                assertEquals(shouldReportMinorSignificance, hasDiagnostic(noAnnotationConstructorDiagnosticRegex(severityForMinor, it)))
            }
            listOf("inlineFoo", "inlineBar").forEach {
                assertEquals(shouldReportMinorSignificance, hasDiagnostic(unboundInlineFunctionSymbolDiagnosticRegex(severityForMinor, it)))
            }

            // Major issues
            assertEquals(shouldReportMajorSignificance, hasDiagnostic(unboundFunctionDiagnosticRegex(severityForMajor)))
            assertEquals(shouldReportMajorSignificance, hasDiagnostic(referenceToUnlinkedConstructorSymbol(severityForMajor)))
        }

        fun String.hasDiagnostic(diagnosticRegex: Regex): Boolean =
            lines().any { it.contains(diagnosticRegex) }

        fun reflectionTargetSymbolDiagnosticRegex(severity: String, kind: String, name: String) =
            Regex("$severity: <lib2>.*Reference to $kind '$name' can not be evaluated: No $kind found for symbol 'lib1/.*")

        fun referenceToUnlinkedConstructorSymbol(severity: String) =
            Regex("$severity: <lib2>.*Reference to constructor 'A.<init>' can not be evaluated: Expression uses unlinked class symbol 'lib1/A.*")

        fun memberOfUnlinkedClassesDiagnosticRegex(severity: String) =
            Regex("$severity: <lib2>.*Class 'KlassWithUnlinkedSymbol' created by constructor 'KlassWithUnlinkedSymbol.<init>' uses unlinked class symbol 'lib1/A.*")

        fun unboundInlineFunctionSymbolDiagnosticRegex(severity: String, name: String) =
            Regex("$severity: <main>.*Expression can not be evaluated: No function found for symbol 'lib1/$name.*")

        fun unboundFunctionDiagnosticRegex(severity: String) =
            Regex("$severity: <lib2>.*Function 'foo' can not be called: No function found for symbol 'lib1/foo.*")

        fun unusableAnnotationDiagnosticRegex(severity: String, name: String) =
            Regex("$severity: <lib2>.*Unusable annotation '$name.<init>' has been removed from class 'KlassWithUnlinkedSymbol'")

        fun noAnnotationConstructorDiagnosticRegex(severity: String, name: String) =
            Regex("$severity: <lib2>.*Constructor '$name.<init>' can not be called: No constructor found for symbol 'lib1/$name.<init>.*")

        fun unusableAnnotationConstructorDiagnosticRegex(severity: String) =
            Regex(
                "$severity: <lib2>.*Constructor 'NoDefault.<init>' can not be called: The constructor has some value" +
                        " parameters for which neither the call site provides an argument, nor do they have a default value: p"
            )

        private val lib11SourceText = """
        |package lib1
        |
        |fun foo() = 42
        |
        |val x = 42
        |
        |fun String.extensionFoo() = 42
        |
        |inline fun inlineFoo(block: () -> Int): Int = block()
        |
        |inline fun inlineBar(noinline block: () -> Int): Int = block()
        |
        |open class A
        |
        |class B(val qux: Int)
        |
        |annotation class Removed
        |
        |annotation class Changed(val p: String)
        |
        |annotation class NoDefault(val p: Int = 42)
    """.trimMargin()

        private val lib12SourceText = """
        |package lib1
        |
        |class B
        |
        |annotation class Changed(val p: Int)
        |
        |annotation class NoDefault(val p: Int)
    """.trimMargin()

        private val lib2SourceText = """
        |package lib2
        |import lib1.*
        |
        |fun removedFun() = foo()
        |
        |inline fun removedFunInInlinedBlock() = inlineFoo { 42 }
        |
        |inline fun removedFunWithNoInlineParamInInlinedBlock() = inlineBar { 42 }
        |
        |fun removedFunName() = ::foo.name
        |
        |fun removedClassName() = ::A.name
        |
        |fun removedPropertyName() = B::qux.name
        |
        |fun removedExtensionFunName() = String::extensionFoo.name
        |
        |@Removed
        |@Changed("42")
        |@NoDefault()
        |class KlassWithUnlinkedSymbol : A() {
        |    companion object
        |}
    """.trimMargin()

        private val mainSourceText = """
        |import lib2.*
        |
        |fun main() {
        |   // Minor issues
        |   removedFunInInlinedBlock()
        |   removedFunWithNoInlineParamInInlinedBlock()
        |   removedFunName()
        |   removedPropertyName()
        |   removedExtensionFunName()
        |   KlassWithUnlinkedSymbol
        |   
        |   // Major issues
        |   removedFun()
        |   removedClassName()
        |}
    """.trimMargin()
    }
}

@UsePartialLinkage(UsePartialLinkage.Mode.SILENT)
class NativePartialLinkageSilentDiagnosticsTest : NativePartialLinkageDiagnosticsTest() {
    @Test
    @DisplayName("Test partial linkage silent diagnostics")
    fun testPartialLinkageSilentDiagnostics() {
        val output = prepareExecutable().assertSuccess().toolOutput()
        output.assertDiagnostics(shouldReportMinorSignificance = false, shouldReportMajorSignificance = false)
    }
}

@UsePartialLinkage(UsePartialLinkage.Mode.INFO)
class NativePartialLinkageInfoDiagnosticsTest : NativePartialLinkageDiagnosticsTest() {
    @Test
    @DisplayName("Test partial linkage info diagnostics")
    fun testPartialLinkageInfoDiagnostics() {
        val output = prepareExecutable().assertSuccess().toolOutput()
        output.assertDiagnostics(shouldReportMinorSignificance = true, shouldReportMajorSignificance = true)
    }
}

@UsePartialLinkage(UsePartialLinkage.Mode.WARNING)
class NativePartialLinkageWarningDiagnosticsTest : NativePartialLinkageDiagnosticsTest() {
    @Test
    @DisplayName("Test partial linkage warning diagnostics")
    fun testPartialLinkageWarningDiagnostics() {
        val output = prepareExecutable().assertSuccess().toolOutput()
        output.assertDiagnostics(shouldReportMinorSignificance = false, shouldReportMajorSignificance = true)
    }
}

@UsePartialLinkage(UsePartialLinkage.Mode.ERROR)
class NativePartialLinkageErrorDiagnosticsTest : NativePartialLinkageDiagnosticsTest() {
    @Test
    @DisplayName("Test partial linkage error diagnostics")
    fun testPartialLinkageErrorDiagnostics() {
        val result = prepareExecutable()
        assertIs<TestCompilationResult.CompilationToolFailure>(result)

        result.toolOutput().assertDiagnostics(
            shouldReportMinorSignificance = true,
            shouldReportMajorSignificance = true,
            shouldReportAsError = true,
        )
    }
}