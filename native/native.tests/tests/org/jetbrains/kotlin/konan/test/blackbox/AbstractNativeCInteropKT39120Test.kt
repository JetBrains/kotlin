/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.DEFAULT_FILE_NAME
import org.jetbrains.kotlin.konan.test.blackbox.support.util.DEFAULT_MODULE_NAME
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpMetadata
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import kotlin.test.assertIs

@Tag("cinterop")
abstract class AbstractNativeCInteropKT39120Test : AbstractNativeCInteropBaseTest() {

    @Synchronized
    protected fun runTest(@TestDataFile testPath: String) {
        // KT-39120 is about Objective-C, so this test is for Apple hosts/targets only
        Assumptions.assumeTrue(targets.hostTarget.family.isAppleFamily && targets.testTarget.family.isAppleFamily)

        val testPathFull = getAbsoluteFile(testPath)
        val testDataDir = testPathFull.parentFile.parentFile
        val def1File = testPathFull.resolve("pod1.def")
        val def2File = testPathFull.resolve("pod2.def")
        val golden1File = testPathFull.resolve("pod1.contents.gold.txt")
        val golden2File = testPathFull.resolve("pod2.contents.gold.txt")

        val includeFrameworkArgs = TestCInteropArgs("-compiler-option", "-F${testDataDir.canonicalPath}")
        val klib1: KLIB = cinteropToLibrary(targets, def1File, buildDir, includeFrameworkArgs).assertSuccess().resultingArtifact
        val metadata1 = klib1.dumpMetadata(kotlinNativeClassLoader.classLoader, false, null)
        val actualFiltered1Output = filterContentsOutput(metadata1, " pod.Version|POD|class Pod")
        assertEqualsToFile(golden1File, actualFiltered1Output)

        val cinterop2ExtraArgs = TestCInteropArgs("-l", klib1.klibFile.canonicalPath, "-compiler-option", "-fmodules")
        val klib2: KLIB = cinteropToLibrary(targets, def2File, buildDir, includeFrameworkArgs + cinterop2ExtraArgs).assertSuccess().resultingArtifact
        val metadata2 = klib2.dumpMetadata(kotlinNativeClassLoader.classLoader, false, null)
        val actualFiltered2Output = filterContentsOutput(metadata2, " pod.Version|POD|class Pod")
        assertEqualsToFile(golden2File, actualFiltered2Output)

        val ktFile = testPathFull.resolve(DEFAULT_FILE_NAME)
        if (ktFile.exists()) {
            // Just compile "main.kt" with klib1 and klib2, without running resulting executable
            val module = TestModule.Exclusive(DEFAULT_MODULE_NAME, emptySet(), emptySet(), emptySet()).apply {
                files += TestFile.createCommitted(ktFile, this)
            }
            // KT-39120 is irrelevant to compiler backend, so executable can be compiled in the simplest way, without splitting to stages.
            val compilationResult = compileToExecutableInOneStage(
                createTestCaseNoTestRun(module, TestCompilerArgs.EMPTY),
                klib1.asLibraryDependency(),
                klib2.asLibraryDependency()
            )

            val expectedFailureTxtFile = testPathFull.resolve("expected.compilation.failure.txt")
            if (expectedFailureTxtFile.exists()) {
                assertIs<TestCompilationResult.CompilationToolFailure>(compilationResult)
                val expectedFailureSubstring = expectedFailureTxtFile.readText()
                val actualFailure = compilationResult.loggedData.toString()
                assert(actualFailure.contains(expectedFailureSubstring)) {
                    "Expected failure substring:\n$expectedFailureSubstring\nActual failure logged data:\n$actualFailure"
                }
            } else {
                compilationResult.assertSuccess()
            }
        }
    }

    private fun filterContentsOutput(contents: String, pattern: String): String {
        val regex = Regex(pattern)
        return contents.lineSequence()
            .filter { it.contains(regex) }
            .filterNot { it.trim() == "@kotlinx/cinterop/ExperimentalForeignApi" }
            .joinToString("\n")
    }

    private fun createTestCaseNoTestRun(module: TestModule.Exclusive, compilerArgs: TestCompilerArgs) = TestCase(
        id = TestCaseId.Named(module.name),
        kind = TestKind.STANDALONE_NO_TR,
        modules = setOf(module),
        freeCompilerArgs = compilerArgs,
        nominalPackageName = PackageName.EMPTY,
        checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
        extras = TestCase.NoTestRunnerExtras(".${module.name}")
    ).apply {
        initialize(null, null)
    }
}
