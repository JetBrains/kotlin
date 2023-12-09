/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ObjCFrameworkCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.ExecutionTimeout
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.LLDB
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PipelineType
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ClangDistribution
import org.jetbrains.kotlin.konan.test.blackbox.support.util.LLDBSessionSpec
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClang
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File


@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
// FIXME: With -opt these tests can't set a breakpoint in inlined "fun bar()"
@EnforcedProperty(ClassLevelProperty.OPTIMIZATION_MODE, propertyValue = "DEBUG")
class ObjCToKotlinSteppingInLLDBTest : AbstractNativeSimpleTest() {

    @Test
    fun stepInFromObjCToKotlin___WithDisabledStopHook___StopsAtABridgingRoutine() {
        testSteppingFromObjcToKotlin(
            """
            > b ${CLANG_FILE_NAME}:3
            > env KONAN_LLDB_DONT_SKIP_BRIDGING_FUNCTIONS=1
            > run
            > thread step-in
            [..] stop reason = step in
            [..]`objc2kotlin_kfun:#bar(){} at <compiler-generated>:1
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            KOTLIN_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepInFromObjCToKotlin___WithDisabledStopHook___StopsAtABridgingRoutine.name}"
        )
    }

    @Test
    fun stepInFromObjCToKotlin___WithStopHook___StepsThroughToKotlinCode() {
        testSteppingFromObjcToKotlin(
            """
            > b ${CLANG_FILE_NAME}:3
            > run
            > thread step-in
            [..] stop reason = Python thread plan implemented by class konan_lldb.KonanStepIn.
            [..]`kfun:#bar(){} at lib.kt:1:1
            -> 1   	fun bar() {
                    ^
               2   	    print("")
               3   	}
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            KOTLIN_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepInFromObjCToKotlin___WithStopHook___StepsThroughToKotlinCode.name}"
        )
    }

    @Test
    fun stepOutFromKotlinToObjC___WithDisabledStopHook___StopsAtABridgingRoutine() {
        testSteppingFromObjcToKotlin(
            """
            > b ${KOTLIN_FILE_NAME}:2
            > env KONAN_LLDB_DONT_SKIP_BRIDGING_FUNCTIONS=1
            > run
            > thread step-out
            [..] stop reason = step out
            [..]`objc2kotlin_kfun:#bar(){} at <compiler-generated>:1
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            KOTLIN_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepOutFromKotlinToObjC___WithDisabledStopHook___StopsAtABridgingRoutine.name}"
        )
    }

    @Test
    fun stepOutFromKotlinToObjC___WithStopHook___StepsOutToObjCCode() {
        testSteppingFromObjcToKotlin(
            """
            > b ${KOTLIN_FILE_NAME}:2
            > run
            > thread step-out
            [..] stop reason = Python thread plan implemented by class konan_lldb.KonanStepOut.
            [..]`main at main.m:3:5
               1   	@import Kotlin;
               2   	int main() {
            -> 3   	    [KotlinLibKt bar];
                        ^
               4   	}
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            KOTLIN_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepOutFromKotlinToObjC___WithStopHook___StepsOutToObjCCode.name}"
        )
    }

    @Test
    fun stepOverFromKotlinToObjC___WithDisabledStopHook___StopsAtABridgingRoutine() {
        testSteppingFromObjcToKotlin(
            """
            > b ${KOTLIN_FILE_NAME}:3
            > env KONAN_LLDB_DONT_SKIP_BRIDGING_FUNCTIONS=1
            > run
            > thread step-over
            [..] stop reason = step over
            [..]`objc2kotlin_kfun:#bar(){} at <compiler-generated>:1
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            KOTLIN_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepOverFromKotlinToObjC___WithDisabledStopHook___StopsAtABridgingRoutine.name}"
        )
    }

    @Test
    fun stepOverFromKotlinToObjC___WithStopHook___StepsOverToObjCCode() {
        testSteppingFromObjcToKotlin(
            """
            > b ${KOTLIN_FILE_NAME}:3
            > run
            > thread step-over
            [..] stop reason = Python thread plan implemented by class konan_lldb.KonanStepOver.
            [..]`main at main.m:3:5
               1   	@import Kotlin;
               2   	int main() {
            -> 3   	    [KotlinLibKt bar];
                        ^
               4   	}
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            KOTLIN_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepOverFromKotlinToObjC___WithStopHook___StepsOverToObjCCode.name}"
        )
    }

    private fun testSteppingFromObjcToKotlin(
        lldbSpec: String,
        clangFileName: String,
        kotlinFileName: String,
        testName: String,
    ) {
        // FIXME: With Rosetta the step-out and step-over tests stop on the line after "[KotlinLibKt bar]"
        if (targets.testTarget != KonanTarget.MACOS_ARM64) { Assumptions.abort<Nothing>("This test is supported only on Apple targets") }

        val kotlinFrameworkName = "Kotlin"
        val clangMainSources = """
            @import ${kotlinFrameworkName};
            int main() {
                [${kotlinFrameworkName}LibKt bar];
            }
        """.trimIndent()

        val kotlinLibrarySources = """
            fun bar() {
                print("")
            }
        """.trimIndent()

        runTestWithLLDB(
            kotlinLibrarySources = kotlinLibrarySources,
            kotlinFileName = kotlinFileName,
            kotlinFrameworkName = kotlinFrameworkName,
            clangMainSources = clangMainSources,
            clangFileName = clangFileName,
            lldbSpec = lldbSpec,
            testName = testName,
        )
    }

    private fun runTestWithLLDB(
        kotlinLibrarySources: String,
        kotlinFileName: String,
        kotlinFrameworkName: String,
        clangMainSources: String,
        clangFileName: String,
        lldbSpec: String,
        testName: String,
    ) {
        // 1. Create sources
        val sourceDirectory = buildDir.resolve("sources")
        sourceDirectory.createDirectory()
        val clangFile = sourceDirectory.resolve(clangFileName)
        clangFile.writeText(clangMainSources)
        sourceDirectory.resolve(kotlinFileName).writeText(kotlinLibrarySources)

        // 2. Build Kotlin framework
        val freeCompilerArgs = TestCompilerArgs(
            testRunSettings.get<PipelineType>().compilerFlags + listOf(
                "-Xstatic-framework",
                "-Xbinary=bundleId=stub",
            )
        )
        val module = generateTestCaseWithSingleModule(sourceDirectory, freeCompilerArgs)
        ObjCFrameworkCompilation(
            testRunSettings,
            freeCompilerArgs = freeCompilerArgs,
            sourceModules = module.modules,
            dependencies = emptyList(),
            expectedArtifact = TestCompilationArtifact.ObjCFramework(
                buildDir,
                kotlinFrameworkName,
            )
        ).result.assertSuccess()

        // 3. Compile the executable
        val clangExecutableName = "clangMain"
        val executableFile = File(buildDir, clangExecutableName)

        val clangResult = compileWithClang(
            // This code was initially written against clang from toolchain.
            // Changing it to another one probably won't hurt, but it was not tested.
            clangDistribution = ClangDistribution.Toolchain,
            sourceFiles = listOf(clangFile),
            outputFile = executableFile,
            frameworkDirectories = listOf(buildDir),
        ).assertSuccess()

        // 4. Generate the test case
        val testExecutable = TestExecutable(
            clangResult.resultingArtifact,
            loggedCompilationToolCall = clangResult.loggedData,
            testNames = listOf(TestName(testName)),
        )
        val spec = LLDBSessionSpec.parse(lldbSpec)
        val moduleForTestCase = TestModule.Exclusive(testName, emptySet(), emptySet(), emptySet())
        val testCase = TestCase(
            id = TestCaseId.Named(testName),
            kind = TestKind.STANDALONE_LLDB,
            modules = setOf(moduleForTestCase),
            freeCompilerArgs = freeCompilerArgs,
            nominalPackageName = PackageName.EMPTY,
            checks = TestRunChecks(
                executionTimeoutCheck = ExecutionTimeout.ShouldNotExceed(testRunSettings.get<Timeouts>().executionTimeout),
                exitCodeCheck = TestRunCheck.ExitCode.Expected(0),
                outputDataFile = null,
                outputMatcher = spec.let { TestRunCheck.OutputMatcher { output -> spec.checkLLDBOutput(output, testRunSettings.get()) } },
                fileCheckMatcher = null,
            ),
            extras = TestCase.NoTestRunnerExtras(
                "main",
                arguments = spec.generateCLIArguments(testRunSettings.get<LLDB>().prettyPrinters)
            )
        ).apply { initialize(null, null) }

        // 5. Run the test
        runExecutableAndVerify(testCase, testExecutable)
    }

    companion object {
        val CLANG_FILE_NAME = "main.m"
        val KOTLIN_FILE_NAME = "lib.kt"
    }

}
