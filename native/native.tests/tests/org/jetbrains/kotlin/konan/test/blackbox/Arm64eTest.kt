/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.test.KtAssert.fail
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.time.Duration

/**
 * Tests `-Xbinary=arm64e=true`. arm64e binaries cannot run on simulators or macOS hosts,
 * so these tests check the generated LLVM IR. They don't use caches, which would move code out of it, unless they test caches.
 */
@TestDataPath("\$PROJECT_ROOT")
@EnforcedProperty(ClassLevelProperty.CACHE_MODE, "NO")
class Arm64eTest : AbstractNativeSimpleTest() {
    private val testSuiteDir = ForTestCompileRuntime.transformTestDataPath("native/native.tests/testData/arm64e")
    private val testCompilationFactory = TestCompilationFactory()

    @BeforeEach
    fun assumeIosArm64() {
        Assumptions.assumeTrue(targets.testTarget == KonanTarget.IOS_ARM64)
    }

    @Test
    fun testTarget() = compileFrameworkAndCheckIr(testSuiteDir.resolve("target.kt"), "LinkBitcodeDependencies")

    @Test
    fun testCFunctionPointers() = compileFrameworkAndCheckIr(
        testSuiteDir.resolve("c_function_pointers.kt"), "Codegen", listOf("-Xbinary=sourceInfoType=libbacktrace")
    )

    @Test
    fun testObjCMessenger() =
        compileFrameworkAndCheckIr(testSuiteDir.resolve("objc/messenger.kt"), "Codegen", givenDependencies = setOf(objcLibrary()))

    /** An Objective-C library without bitcode: with direct C calls, cinterop compiles no stubs. */
    private fun objcLibrary() = TestModule.Given(cinterop("objc", listOf("-Xccall-mode", "direct")).klibFile)

    private fun cinterop(testName: String, cinteropArgs: List<String> = emptyList()): TestCompilationArtifact.KLIB =
        cinteropToLibrary(
            defFile = testSuiteDir.resolve(testName).resolve("$testName.def"),
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs(emptyList(), cinteropArgs = cinteropArgs),
        ).assertSuccess().resultingArtifact

    private fun compileFramework(
        source: File,
        compilerArgs: List<String>,
        givenDependencies: Set<TestModule.Given> = emptySet(),
        checks: TestRunChecks = TestRunChecks.Default(Duration.ZERO),
    ) = testCompilationFactory.testCaseToObjCFrameworkCompilation(
        generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR,
            TestCase.NoTestRunnerExtras("There's no entrypoint in a framework"),
            source.nameWithoutExtension,
            listOf(source),
            TestCompilerArgs(compilerArgs + "-Xbinary=bundleId=${source.nameWithoutExtension}"),
            givenDependencies,
            checks,
        ),
        testRunSettings,
    )

    private fun compileFrameworkAndCheckIr(
        testDataFile: File,
        stage: String,
        compilerArgs: List<String> = emptyList(),
        arm64e: Boolean = true,
        givenDependencies: Set<TestModule.Given> = emptySet(),
    ) {
        val fileCheckMatcher = TestRunCheck.FileCheckMatcher(testRunSettings, testDataFile)
        compileFramework(
            testDataFile,
            listOfNotNull("-Xbinary=arm64e=true".takeIf { arm64e }) + compilerArgs + listOf(
                "-Xsave-llvm-ir-after=$stage",
                "-Xsave-llvm-ir-directory=${buildDir.absolutePath}",
            ),
            givenDependencies,
            // KT-64879: FileCheck is a run check, so it is called directly here.
            TestRunChecks.Default(Duration.ZERO).copy(fileCheckMatcher = fileCheckMatcher),
        ).result.assertSuccess()
        checkIr(testDataFile, stage, fileCheckMatcher)
    }

    private fun checkIr(
        testDataFile: File,
        stage: String,
        fileCheckMatcher: TestRunCheck.FileCheckMatcher = TestRunCheck.FileCheckMatcher(testRunSettings, testDataFile),
    ) {
        val irFile = buildDir.resolve("out.$stage.ll")
        val result = fileCheckMatcher.doFileCheck(irFile)
        if (result.stdout.isNotEmpty() || result.stderr.isNotEmpty()) {
            fail(
                "FileCheck of ${irFile.absolutePath} with '--check-prefixes ${fileCheckMatcher.prefixes}' failed:\n" +
                        "${result.stdout}\n${result.stderr}"
            )
        }
    }
}
