/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataFile
import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.blackboxtest.InfrastructureDumpedTestListingTest.Companion.TEST_SUITE_PATH
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.WithTestRunnerExtras
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.ExistingDependency
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.LibraryCompilation
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.Executable
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependency
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType.IncludedLibrary
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType.Library
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Success
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunners
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.SimpleTestDirectories
import org.jetbrains.kotlin.konan.blackboxtest.support.util.DumpedTestListing
import org.jetbrains.kotlin.konan.blackboxtest.support.util.LAUNCHER_MODULE_NAME
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@Tag("infrastructure")
@EnforcedHostTarget
@TestMetadata(TEST_SUITE_PATH)
@TestDataPath("\$PROJECT_ROOT")
class InfrastructureDumpedTestListingTest : AbstractNativeSimpleTest() {
    @Test
    @TestMetadata(TEST_CASE_NAME)
    fun testListingCompiledFromSources() {
        doTest(TEST_CASE_PATH, fromSources = true)
    }

    @Test
    @TestMetadata(TEST_CASE_NAME)
    fun testListingCompiledFromIncludedLibrary() {
        doTest(TEST_CASE_PATH, fromSources = false)
    }

    @Suppress("SameParameterValue")
    private fun doTest(@TestDataFile testDataPath: String, fromSources: Boolean) {
        val rootDir = File(testDataPath)

        val fooTestCase: TestCase = generateTestCaseWithSingleModule(rootDir.resolve("foo"))
        val fooCompilationResult: Success<out KLIB> = fooTestCase.compileToLibrary()
        val fooLibrary: KLIB = fooCompilationResult.resultingArtifact

        val barTestCase: TestCase = generateTestCaseWithSingleModule(rootDir.resolve("bar"))

        val executableTestCase: TestCase
        val executableCompilationResult: Success<out Executable>

        if (fromSources) {
            executableTestCase = barTestCase
            executableCompilationResult = barTestCase.compileToExecutable(fooLibrary.asLibraryDependency())
        } else {
            val barCompilationResult: Success<out KLIB> = barTestCase.compileToLibrary(fooLibrary.asLibraryDependency())
            val barLibrary: KLIB = barCompilationResult.resultingArtifact

            executableTestCase = generateTestCaseWithSingleModule(moduleDir = null) // No sources.
            executableCompilationResult = executableTestCase.compileToExecutable(
                fooLibrary.asLibraryDependency(),
                barLibrary.asIncludedLibraryDependency()
            )
        }

        val executable: Executable = executableCompilationResult.resultingArtifact

        // check that the test listing dumped during the compilation matches our expectations:
        val testDumpFile = executable.testDumpFile
        assertDumpFilesEqual(expected = rootDir.resolve("expected-test-listing.dump"), actual = testDumpFile)

        // parse test listing that was dumped to a file during compilation:
        val dumpedTestListing = DumpedTestListing.parse(testDumpFile.readText()).toSet()
        assertTrue(dumpedTestListing.isNotEmpty())

        // parse test listing obtained from executable file with the help of --ktest_list_tests flag:
        val testExecutable = TestExecutable(executable.executableFile, executableCompilationResult.loggedData)
        val extractedTestListing = TestRunners.extractTestNames(testExecutable, testRunSettings).toSet()

        assertEquals(extractedTestListing, dumpedTestListing)

        runExecutableAndVerify(executableTestCase, testExecutable) // <-- run executable and verify
    }

    private fun generateTestCaseWithSingleModule(moduleDir: File?): TestCase {
        val moduleName: String = moduleDir?.name ?: LAUNCHER_MODULE_NAME
        val module = TestModule.Exclusive(moduleName, emptySet(), emptySet())

        moduleDir?.walkTopDown()
            ?.filter { it.isFile && it.extension == "kt" }
            ?.forEach { file -> module.files += TestFile.createCommitted(file, module) }

        return TestCase(
            id = TestCaseId.Named(moduleName),
            kind = TestKind.STANDALONE,
            modules = setOf(module),
            freeCompilerArgs = TestCompilerArgs.EMPTY,
            nominalPackageName = PackageName.EMPTY,
            expectedOutputDataFile = null,
            extras = DEFAULT_EXTRAS
        ).apply {
            initialize(null)
        }
    }

    private fun TestCase.compileToLibrary(vararg dependencies: TestCompilationDependency<*>): Success<out KLIB> {
        val compilation = LibraryCompilation(
            settings = testRunSettings,
            freeCompilerArgs = freeCompilerArgs,
            sourceModules = modules,
            dependencies = dependencies.toList(),
            expectedArtifact = toLibraryArtifact()
        )
        return compilation.result.assertSuccess()
    }

    private fun TestCase.compileToExecutable(vararg dependencies: TestCompilationDependency<*>): Success<out Executable> {
        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = freeCompilerArgs,
            sourceModules = modules,
            extras = DEFAULT_EXTRAS,
            dependencies = dependencies.toList(),
            expectedArtifact = toExecutableArtifact()
        )
        return compilation.result.assertSuccess()
    }

    private val buildDir: File get() = testRunSettings.get<SimpleTestDirectories>().testBuildDir

    private fun TestCase.toLibraryArtifact() = KLIB(buildDir.resolve(modules.first().name + ".klib"))
    private fun toExecutableArtifact() =
        Executable(buildDir.resolve("app." + testRunSettings.get<KotlinNativeTargets>().testTarget.family.exeSuffix))

    private fun KLIB.asLibraryDependency() = ExistingDependency(this, Library)
    private fun KLIB.asIncludedLibraryDependency() = ExistingDependency(this, IncludedLibrary)

    private fun assertDumpFilesEqual(expected: File, actual: File) {
        val expectedDumpFileContents = expected.readText().trimEnd()
        val actualDumpFileContents = actual.readText().trimEnd()

        assertEquals(expectedDumpFileContents, actualDumpFileContents) {
            """
                Test dump file contents mismatch.
                Expected: ${expected.absolutePath}
                Actual: ${actual.absolutePath}
            """.trimIndent()
        }
    }

    companion object {
        const val TEST_SUITE_PATH = "native/native.tests/testData/infrastructure"
        const val TEST_CASE_NAME = "testListing"
        const val TEST_CASE_PATH = "$TEST_SUITE_PATH/$TEST_CASE_NAME"

        private val DEFAULT_EXTRAS = WithTestRunnerExtras(TestRunnerType.DEFAULT)
    }
}
