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
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
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
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File


@FirPipeline
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
// FIXME: With -opt these tests can't set a breakpoint in inlined "fun bar()"
@EnforcedProperty(ClassLevelProperty.OPTIMIZATION_MODE, propertyValue = "DEBUG")
class ObjCToKotlinSteppingInLLDBTest : AbstractNativeSimpleTest() {

    @Test
    fun stepInFromObjCToKotlin___WithoutTransparentStepping___StopsAtABridgingRoutine() {
        // `settings set target.enable-trampoline-support false` works wrong in that case:
        // instead of stepping into the bridge, lldb just skips the call entirely.
        // In other words, the effect of this setting is different from not generating
        // the transparent stepping attribute at all.
        //
        // This addition might be the cause:
        // https://github.com/llvm/llvm-project/blob/2af4818d8de2fd1ae2d8c274470b8c0cab2e26ca/lldb/source/Target/ThreadPlanStepThrough.cpp#L36-L39
        //
        // In any case, it is not too bad: even if `settings set target.enable-trampoline-support false`
        // doesn't disable the transparent stepping attribute in some cases, there is still an option
        // to do so with the compiler flag:
        val additionalKotlinCompilerArgs = listOf("-Xbinary=enableDebugTransparentStepping=false")

        testSteppingFromObjcToKotlin(
            """
            > b ${CLANG_FILE_NAME}:4
            > run
            > thread step-in
            [..] stop reason = step in
            [..]`objc2kotlin_kfun:#bar(){} at <compiler-generated>:1
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            KOTLIN_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepInFromObjCToKotlin___WithoutTransparentStepping___StopsAtABridgingRoutine.name}",
            additionalKotlinCompilerArgs = additionalKotlinCompilerArgs
        )
    }

    @Test
    fun stepInFromObjCToKotlin___WithTransparentStepping___StepsThroughToKotlinCode() {
        testSteppingFromObjcToKotlin(
            """
            > b ${CLANG_FILE_NAME}:3
            > run
            > thread step-in
            [..]`kfun:#bar(){} at lib.kt:1:1
            -> 1   	fun bar() {
                    ^
               2   	    print("")
               3   	}
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            KOTLIN_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepInFromObjCToKotlin___WithTransparentStepping___StepsThroughToKotlinCode.name}"
        )
    }

    @Test
    fun stepInFromObjCToVirtualKotlin___WithTransparentStepping___StepsThroughToKotlinCode() {
        val clangMainSources = """
            @import ${kotlinFrameworkName};
            void landing() {}
            int main() {
                id<${kotlinFrameworkName}Foo> foo = [${kotlinFrameworkName}LibKt createFoo];
                [foo foo];
                landing();
            }
        """.trimIndent()
        val kotlinLibrarySources = """
            interface Foo {
                fun foo()
            }
            fun createFoo(): Foo = Bar()
            private class Bar : Foo {
                override fun foo() {
                    print("")
                }
            }
        """.trimIndent()
        testSteppingFromObjcToKotlin(
            """
            > b ${CLANG_FILE_NAME}:5
            > run
            > thread step-in
            [..]`kfun:Bar.foo#internal(_this=[]) at lib.kt:6:5
               3   	}
               4   	fun createFoo(): Foo = Bar()
               5   	private class Bar : Foo {
            -> 6   	    override fun foo() {
                	    ^
               7   	        print("")
               8   	    }
               9   	}
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            KOTLIN_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepInFromObjCToVirtualKotlin___WithTransparentStepping___StepsThroughToKotlinCode.name}",
            clangMainSources = clangMainSources,
            kotlinLibrarySources = kotlinLibrarySources,
        )
    }

    @Test
    fun stepOutFromKotlinToObjC___WithoutTransparentStepping___StopsAtABridgingRoutine() {
        testSteppingFromObjcToKotlin(
            """
            > b ${KOTLIN_FILE_NAME}:2
            > settings set target.enable-trampoline-support false
            > run
            > thread step-out
            [..] stop reason = step out
            [..]`objc2kotlin_kfun:#bar(){} at <compiler-generated>:1
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            KOTLIN_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepOutFromKotlinToObjC___WithoutTransparentStepping___StopsAtABridgingRoutine.name}"
        )
    }

    @Test
    fun stepOutFromKotlinToObjC___WithTransparentStepping___StepsOutToObjCCode() {
        testSteppingFromObjcToKotlin(
            """
            > b ${KOTLIN_FILE_NAME}:2
            > run
            > thread step-out
            [..]`main at main.m:5:5
               2   	void landing() {}
               3   	int main() {
               4   	    [KotlinLibKt bar];
            -> 5   	    landing();
                        ^
               6   	}
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            KOTLIN_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepOutFromKotlinToObjC___WithTransparentStepping___StepsOutToObjCCode.name}"
        )
    }

    @Test
    fun stepOverFromKotlinToObjC___WithoutTransparentStepping___StopsAtABridgingRoutine() {
        testSteppingFromObjcToKotlin(
            """
            > b ${KOTLIN_FILE_NAME}:3
            > settings set target.enable-trampoline-support false
            > run
            > thread step-over
            [..] stop reason = step over
            [..]`objc2kotlin_kfun:#bar(){} at <compiler-generated>:1
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            KOTLIN_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepOverFromKotlinToObjC___WithoutTransparentStepping___StopsAtABridgingRoutine.name}"
        )
    }

    @Test
    fun stepOverFromKotlinToObjC___WithTransparentStepping___StepsOverToObjCCode() {
        testSteppingFromObjcToKotlin(
            """
            > b ${KOTLIN_FILE_NAME}:3
            > run
            > thread step-over
            [..]`main at main.m:5:5
               2   	void landing() {}
               3   	int main() {
               4   	    [KotlinLibKt bar];
            -> 5   	    landing();
                        ^
               6   	}
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            KOTLIN_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepOverFromKotlinToObjC___WithTransparentStepping___StepsOverToObjCCode.name}"
        )
    }

    private val kotlinFrameworkName: String = "Kotlin"

    private fun testSteppingFromObjcToKotlin(
        lldbSpec: String,
        clangFileName: String,
        kotlinFileName: String,
        testName: String,
        additionalKotlinCompilerArgs: List<String> = emptyList(),
        clangMainSources: String = """
            @import ${kotlinFrameworkName};
            void landing() {}
            int main() {
                [${kotlinFrameworkName}LibKt bar];
                landing();
            }
        """.trimIndent(),
        kotlinLibrarySources: String = """
            fun bar() {
                print("")
            }
        """.trimIndent(),
    ) {
        // FIXME: With Rosetta the step-out and step-over tests stop on the line after "[KotlinLibKt bar]"
        if (targets.testTarget != KonanTarget.MACOS_ARM64) { Assumptions.abort<Nothing>("This test is supported only on Apple targets") }

        runTestWithLLDB(
            kotlinLibrarySources = kotlinLibrarySources,
            kotlinFileName = kotlinFileName,
            kotlinFrameworkName = kotlinFrameworkName,
            clangMainSources = clangMainSources,
            clangFileName = clangFileName,
            lldbSpec = lldbSpec,
            testName = testName,
            additionalKotlinCompilerArgs = additionalKotlinCompilerArgs,
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
        additionalKotlinCompilerArgs: List<String>,
    ) {
        // 1. Create sources
        val sourceDirectory = buildDir.resolve("sources")
        sourceDirectory.createDirectory()
        val clangFile = sourceDirectory.resolve(clangFileName)
        clangFile.writeText(clangMainSources)
        sourceDirectory.resolve(kotlinFileName).writeText(kotlinLibrarySources)

        // 2. Build Kotlin framework
        val freeCompilerArgs = TestCompilerArgs(
            testRunSettings.get<PipelineType>().compilerFlags + additionalKotlinCompilerArgs + listOf(
                "-Xstatic-framework",
                "-Xbinary=bundleId=stub",
                "-module-name", kotlinFrameworkName
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
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).copy(
                outputMatcher = spec.let { TestRunCheck.OutputMatcher { output -> spec.checkLLDBOutput(output, testRunSettings.get()) } }
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
