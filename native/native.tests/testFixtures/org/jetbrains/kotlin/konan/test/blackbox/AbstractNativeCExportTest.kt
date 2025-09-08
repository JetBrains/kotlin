/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.PackageName
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseId
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestFile
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import org.jetbrains.kotlin.konan.test.blackbox.support.util.*
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ClangDistribution
import org.jetbrains.kotlin.konan.test.blackbox.support.util.DEFAULT_MODULE_NAME
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClang
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("cexport")
abstract class AbstractNativeCExportTest() : AbstractNativeSimpleTest() {

    internal open fun checkTestPrerequisites() {
        testRunSettings.assumeLibraryKindSupported()
    }

    private val testCompilationFactory = TestCompilationFactory()

    protected fun runTest(@TestDataFile testDir: String) {
        checkTestPrerequisites()

        // https://youtrack.jetbrains.com/issue/KT-69303
        if (testDir == "native/native.tests/testData/CExport/InterfaceV1/concurrentTerminate/" && HostManager.hostIsMac) {
            Assumptions.abort<Nothing>("concurrentTerminate flaks on Mac, see KT-69303")
        }

        // https://youtrack.jetbrains.com/issue/KT-69663
        if (testDir == "native/native.tests/testData/CExport/InterfaceV1/unhandledExceptionThroughBridge/" && HostManager.hostIsMingw) {
            Assumptions.abort<Nothing>("unhandledExceptionThroughBridge flaks on Windows, see KT-69663")
        }

        val testPathFull = getAbsoluteFile(testDir)
        val ktSources = testPathFull.list()!!
            .filter { it.endsWith(".kt") }
            .map { testPathFull.resolve(it) }
        ktSources.forEach { muteTestIfNecessary(it) }

        val (clangMode, cSources) = run {
            val cSources = testPathFull.list()!!
                .filter { it.endsWith(".c") }
                .map { testPathFull.resolve(it) }

            val cppSources = testPathFull.list()!!
                .filter { it.endsWith(".cpp") }
                .map { testPathFull.resolve(it) }

            if (cSources.isNotEmpty() && cppSources.isNotEmpty()) {
                error("CExportTest does not support mixing .c and .cpp files")
            }

            if (cppSources.isEmpty()) {
                ClangMode.C to cSources
            } else {
                ClangMode.CXX to cppSources
            }
        }

        val goldenData = testPathFull.list()!!
            .singleOrNull { it.endsWith(".out") }
            ?.let { testPathFull.resolve(it) }

        val regexes = testPathFull.list()!!
            .singleOrNull { it.endsWith(".out.re") }
            ?.let { testPathFull.resolve(it) }

        val exitCode = testPathFull.list()!!
            .singleOrNull { it == "exitCode" }
            ?.let { testPathFull.resolve(it).readText() }

        val testCase = generateCExportTestCase(testPathFull, ktSources, goldenData = goldenData, regexes = regexes, exitCode = exitCode)
        val binaryLibrary = testCompilationFactory.testCaseToBinaryLibrary(
            testCase,
            testRunSettings,
            kind = testRunSettings.get<BinaryLibraryKind>(),
        ).result.assertSuccess().resultingArtifact

        val clangExecutableName = "clangMain"
        // We create executable in the same directory as dynamic library because there is no rpath on Windows.
        // Possible alternative: generate executable in buildDir and move or copy DLL there.
        // It might make sense in case of multiple dynamic libraries, but let's keep things simple for now.
        val executableFile = File(binaryLibrary.libraryFile.parentFile, clangExecutableName)
        val includeDirectories = binaryLibrary.headerFile?.let { listOf(it.parentFile) } ?: emptyList()
        val libraryName = binaryLibrary.libraryFile.nameWithoutExtension.substringAfter("lib")
        val clangResult = compileWithClang(
            clangMode = clangMode,
            sourceFiles = cSources,
            includeDirectories = includeDirectories,
            outputFile = executableFile,
            libraryDirectories = listOf(binaryLibrary.libraryFile.parentFile),
            libraries = listOf(libraryName),
            additionalClangFlags = testRunSettings.getKindSpecificClangFlags(binaryLibrary) + listOf("-Wall", "-Werror"),
        ).assertSuccess()

        val testExecutable = TestExecutable(
            clangResult.resultingArtifact,
            loggedCompilationToolCall = clangResult.loggedData,
            testNames = listOf(TestName("TMP")),
        )

        runExecutableAndVerify(testCase, testExecutable)
    }

    private fun generateCExportTestCase(testPathFull: File, sources: List<File>, goldenData: File? = null, regexes: File? = null, exitCode: String? = null): TestCase {
        val moduleName: String = testPathFull.name
        val module = TestModule.Exclusive(moduleName, emptySet(), emptySet(), emptySet())
        sources.forEach { module.files += TestFile.createCommitted(it, module) }

        return TestCase(
            id = TestCaseId.Named(moduleName),
            kind = TestKind.STANDALONE_NO_TR,
            modules = setOf(module),
            freeCompilerArgs = TestCompilerArgs(listOf(
                "-opt-in", "kotlin.experimental.ExperimentalNativeApi",
                "-opt-in", "kotlinx.cinterop.ExperimentalForeignApi",
                "-opt-in", "kotlin.native.internal.InternalForKotlinNative",
            )),
            nominalPackageName = PackageName(moduleName),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).run {
                copy(
                    outputDataFile = goldenData?.let { TestRunCheck.OutputDataFile(file = it) },
                    outputMatcher = regexes?.let { regexesFile ->
                        val regexes = regexesFile.readLines().map { it.toRegex(RegexOption.DOT_MATCHES_ALL) }
                        TestRunCheck.OutputMatcher {
                            regexes.forEach { regex ->
                                assertTrue(regex.matches(it)) {
                                    "Regex `$regex` failed to match `$it`"
                                }
                            }
                            true
                        }
                    },
                    exitCodeCheck = exitCode?.let {
                        if (it == "!0") {
                            TestRunCheck.ExitCode.AnyNonZero
                        } else {
                            TestRunCheck.ExitCode.Expected(it.toInt())
                        }
                    } ?: exitCodeCheck
                )
            },
            extras = TestCase.NoTestRunnerExtras(entryPoint = "main")
        ).apply {
            initialize(null, null)
        }
    }
}