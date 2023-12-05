/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
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
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import org.jetbrains.kotlin.konan.test.blackbox.support.util.DEFAULT_MODULE_NAME
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClang
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("cexport")
abstract class AbstractNativeCExportTest(
    protected val libraryKind: BinaryLibraryKind,
) : AbstractNativeSimpleTest() {

    enum class BinaryLibraryKind {
        STATIC, DYNAMIC
    }

    protected open val kindSpecificClangFlags: List<String> = emptyList()

    private val testCompilationFactory = TestCompilationFactory()

    protected fun runTest(@TestDataFile testDir: String) {
        val testPathFull = getAbsoluteFile(testDir)
        val ktSources = testPathFull.list()!!
            .filter { it.endsWith(".kt") }
            .map { testPathFull.resolve(it) }
        ktSources.forEach { muteTestIfNecessary(it) }

        val cSources = testPathFull.list()!!
            .filter { it.endsWith(".c") }
            .map { testPathFull.resolve(it) }

        val goldenData = testPathFull.list()!!
            .singleOrNull { it.endsWith(".out") }
            ?.let { testPathFull.resolve(it) }

        val testCase = generateCExportTestCase(testPathFull, ktSources, goldenData = goldenData)
        val binaryLibrary = testCompilationFactory.testCaseToBinaryLibrary(
            testCase,
            testRunSettings,
            kind = libraryKind.mapToArtifactKind(),
        ).result.assertSuccess().resultingArtifact

        val clangExecutableName = "clangMain"
        val executableFile = File(buildDir, clangExecutableName)
        val includeDirectories = binaryLibrary.headerFile?.let { listOf(it.parentFile) } ?: emptyList()
        val libraryName = binaryLibrary.libraryFile.nameWithoutExtension.substringAfter("lib")
        compileWithClang(
            sourceFiles = cSources,
            includeDirectories = includeDirectories,
            outputFile = executableFile,
            libraryDirectories = listOf(binaryLibrary.libraryFile.parentFile),
            libraries = listOf(libraryName),
            additionalLinkerFlags = kindSpecificClangFlags,
        )

        val testExecutable = TestExecutable(
            TestCompilationArtifact.Executable(executableFile),
            loggedCompilationToolCall = LoggedData.NoopCompilerCall(buildDir),
            testNames = listOf(TestName("TMP")),
        )

        runExecutableAndVerify(testCase, testExecutable)
    }

    private fun generateCExportTestCase(testPathFull: File, sources: List<File>, goldenData: File? = null): TestCase {
        val moduleName: String = testPathFull.name
        val module = TestModule.Exclusive(DEFAULT_MODULE_NAME, emptySet(), emptySet(), emptySet())
        sources.forEach { module.files += TestFile.createCommitted(it, module) }

        return TestCase(
            id = TestCaseId.Named(moduleName),
            kind = TestKind.STANDALONE_NO_TR,
            modules = setOf(module),
            freeCompilerArgs = TestCompilerArgs(listOf()),
            nominalPackageName = PackageName(moduleName),
            checks = TestRunChecks(
                executionTimeoutCheck = TestRunCheck.ExecutionTimeout.ShouldNotExceed(testRunSettings.get<Timeouts>().executionTimeout),
                exitCodeCheck = TestRunCheck.ExitCode.Expected(0),
                expectedFailureCheck = null,
                outputDataFile = goldenData?.let { TestRunCheck.OutputDataFile(it) },
                outputMatcher = null,
                fileCheckMatcher = null,
            ),
            extras = TestCase.NoTestRunnerExtras(entryPoint = "main")
        ).apply {
            initialize(null, null)
        }
    }

    private fun BinaryLibraryKind.mapToArtifactKind(): TestCompilationArtifact.BinaryLibrary.Kind = when (this) {
        BinaryLibraryKind.STATIC -> TestCompilationArtifact.BinaryLibrary.Kind.STATIC
        BinaryLibraryKind.DYNAMIC -> TestCompilationArtifact.BinaryLibrary.Kind.DYNAMIC
    }
}

abstract class AbstractNativeCExportStaticTest() : AbstractNativeCExportTest(libraryKind = BinaryLibraryKind.STATIC) {
    override val kindSpecificClangFlags: List<String>
        get() = testRunSettings.configurables.linkerKonanFlags.flatMap { listOf("-Xlinker", it) }
}
abstract class AbstractNativeCExportDynamicTest() : AbstractNativeCExportTest(libraryKind = BinaryLibraryKind.DYNAMIC)