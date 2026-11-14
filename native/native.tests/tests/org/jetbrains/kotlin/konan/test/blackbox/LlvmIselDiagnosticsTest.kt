/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.TestFile
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.native.executors.runProcess
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

@TestDataPath("\$PROJECT_ROOT")
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
class LlvmIselDiagnosticsTest : AbstractNativeLinkerOutputTest() {
    private val testDir = ForTestCompileRuntime.transformTestDataPath("native/native.tests/testData/llvmIsel")

    @Test
    fun `fast isel on Apple arm64 targets reports a strong warning`() {
        val target = testRunSettings.get<KotlinNativeTargets>().testTarget
        Assumptions.assumeTrue(
            target.family.isAppleFamily && target.architecture == Architecture.ARM64,
            "The warning is only reported for Apple arm64 targets",
        )

        val compilationResult = compileExecutable(extraArgs = listOf("-Xllvm-isel=fast"))
        assertTrue(compilationResult is TestCompilationResult.Success, "Compilation is expected to succeed")
        val compilationOutput = compilationResult.loggedData.toString()

        assertTrue(
            "known to miscompile sign extension" in compilationOutput,
            "A strong warning about possible runtime failures must be reported for -Xllvm-isel=fast on Apple arm64 targets.\nActual output:\n$compilationOutput"
        )
    }

    @Test
    fun `the konan properties default selector reaches the clang invocation`() {
        val target = testRunSettings.get<KotlinNativeTargets>().testTarget
        Assumptions.assumeTrue(
            target.family.isAppleFamily && target.architecture == Architecture.ARM64,
            "Only Apple arm64 targets have a selector configured in konan.properties",
        )

        // The clang invocation is only logged by the CLI compiler, so run konanc directly.
        val konanc = testRunSettings.get<KotlinNativeHome>().dir.resolve("bin").resolve("konanc")
        val kexe = buildDir.resolve("llvmIselDefaultSelector.kexe").also { it.delete() }
        val result = runProcess(
            konanc.absolutePath,
            testDir.resolve("llvmIsel.kt").absolutePath,
            "-target", target.visibleName,
            "-entry", "llvmIsel",
            "-output", kexe.absolutePath,
            "-Xverbose-phases=ObjectFiles",
        ) {
            timeout = 15.minutes
            environment["JAVA_HOME"] = System.getProperty("java.home")
        }

        assertTrue(
            "-fast-isel=false" in result.stderr && "-global-isel=false" in result.stderr,
            "The per-target 'llvmInstructionSelector' default must reach the clang invocation.\nActual output:\n${result.stderr}"
        )
    }

    @Test
    fun `default isel defers to the konan configuration without warnings`() {
        val compilationResult = compileExecutable(extraArgs = listOf("-Xllvm-isel=default"))
        assertTrue(compilationResult is TestCompilationResult.Success, "Compilation is expected to succeed")
        val compilationOutput = compilationResult.loggedData.toString()

        assertTrue(
            "known to miscompile sign extension" !in compilationOutput,
            "'default' must not be reported as a warning-worthy selector, as it only defers to the konan.properties configuration.\nActual output:\n$compilationOutput"
        )
    }

    @Test
    fun `unsupported isel value is reported as an error`() {
        val compilationResult = compileExecutable(extraArgs = listOf("-Xllvm-isel=bogus"))
        assertTrue(compilationResult is TestCompilationResult.Failure, "Compilation is expected to fail")
        val compilationOutput = compilationResult.loggedData.toString()

        assertTrue(
            compilationOutput.contains("Unsupported -Xllvm-isel=bogus", ignoreCase = true),
            "An unsupported -Xllvm-isel value must be reported as an error.\nActual output:\n$compilationOutput"
        )
    }

    private fun compileExecutable(extraArgs: List<String>): TestCompilationResult<out TestCompilationArtifact.Executable> {
        val module = TestModule.Exclusive("llvmIsel", emptySet(), emptySet(), emptySet()).apply {
            files += TestFile.createCommitted(testDir.resolve("llvmIsel.kt"), this)
        }

        return compileToExecutable(
            module,
            emptyList(),
            extraArgs,
        )
    }
}
