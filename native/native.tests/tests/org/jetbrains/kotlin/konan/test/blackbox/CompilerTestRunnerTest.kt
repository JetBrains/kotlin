/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.OutputDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull

abstract class CompilerTestRunnerTestBase : AbstractNativeSimpleTest() {
    private val testRoot = File("native/native.tests/testData/testRunner")

    private fun runTest(
        name: String,
        files: List<File>,
        compilationArgs: List<String> = emptyList(),
        executionArgs: List<String> = emptyList(),
        outputDataFile: OutputDataFile? = null,
        outputMatcher: TestRunCheck.OutputMatcher? = null,
        dependencies: List<TestCompilationArtifact.KLIB> = emptyList(),
    ) {
        val module = TestModule.Exclusive(name, emptySet(), emptySet(), emptySet()).apply {
            files.forEach {
                this.files += TestFile.createCommitted(it, this)
            }
        }
        val checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).run {
            if (outputDataFile != null) {
                copy(outputDataFile = outputDataFile)
            } else if (outputMatcher != null) {
                copy(outputMatcher = outputMatcher)
            } else {
                this
            }
        }
        val testCase = TestCase(
            id = TestCaseId.Named(module.name),
            kind = TestKind.STANDALONE_NO_TR,
            modules = setOf(module),
            freeCompilerArgs = TestCompilerArgs(listOf("-tr") + compilationArgs),
            nominalPackageName = PackageName.EMPTY,
            checks = checks,
            extras = TestCase.NoTestRunnerExtras(arguments = executionArgs),
        ).apply {
            initialize(null, null)
        }
        val outputDirectory = buildDir.resolve(name)
        outputDirectory.mkdirs()
        val compilationResult = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = dependencies.map { it.asLibraryDependency() },
            expectedArtifact = TestCompilationArtifact.Executable(outputDirectory.resolve("app." + testRunSettings.get<KotlinNativeTargets>().testTarget.family.exeSuffix)),
            tryPassSystemCacheDirectory = true,
        ).result.assertSuccess()
        val executable = TestExecutable.fromCompilationResult(testCase, compilationResult)
        runExecutableAndVerify(testCase, executable)
    }

    @Test
    fun testGlobalInitializers() {
        runTest(
            "globalInitializers",
            listOf(
                testRoot.resolve("globalInitializers/lib.kt"),
                testRoot.resolve("globalInitializers/lib2.kt"),
                testRoot.resolve("globalInitializers/main.kt")
            ),
        )
    }

    @Test
    fun testAnnotations() {
        runTest(
            "annotations",
            listOf(testRoot.resolve("annotations.kt")),
            executionArgs = listOf("--ktest_logger=SIMPLE"),
            outputDataFile = OutputDataFile(file = testRoot.resolve("annotations.out"))
        )
    }

    @Test
    fun testCustomMain() {
        runTest(
            "custom_main",
            listOf(testRoot.resolve("custom_main.kt")),
            compilationArgs = listOf("-e", "kotlin.test.tests.main"),
            executionArgs = listOf("--ktest_logger=SIMPLE", "--ktest_repeat=2"),
            outputDataFile = OutputDataFile(file = testRoot.resolve("custom_main.out"))
        )
    }

    @Test
    fun testWithLibrary() {
        val library = compileToLibrary(testRoot.resolve("withLibrary/lib"))
        runTest(
            "withLibrary",
            listOf(testRoot.resolve("withLibrary/library_user.kt")),
            compilationArgs = listOf("-e", "main"),
            executionArgs = listOf("--ktest_logger=SILENT"),
            dependencies = listOf(library)
        )
    }

    @Test
    fun testStacktrace() {
        runTest("stacktrace",
                listOf(testRoot.resolve("stacktrace.kt")),
                executionArgs = listOf("--ktest_logger=TEAMCITY", "--ktest_no_exit_code"),
                outputMatcher = TestRunCheck.OutputMatcher { rawOutput ->
                    val output = rawOutput.replace("\r\n", "\n")
                    val ignoredOutput = """
            |##teamcity[testSuiteStarted name='kotlin.test.tests.Ignored' locationHint='ktest:suite://kotlin.test.tests.Ignored']
            |##teamcity[testIgnored name='foo']
            |##teamcity[testSuiteFinished name='kotlin.test.tests.Ignored']""".trimMargin().replace("\r\n", "\n")
                    val failedOutput = """
            |##teamcity[testSuiteStarted name='kotlin.test.tests.Failed' locationHint='ktest:suite://kotlin.test.tests.Failed']
            |##teamcity[testStarted name='bar' locationHint='ktest:test://kotlin.test.tests.Failed.bar']
            |##teamcity[testFailed name='bar' message='Bar' details='kotlin.Exception: Bar|n""".trimMargin().replace("\r\n", "\n")

                    assertContains(output, ignoredOutput)
                    assertContains(output, failedOutput)
                    val line = output.lines().find { it.startsWith("##teamcity[testFailed name='bar'") }
                    assertNotNull(line)
                    assertContains(line, "Caused by: kotlin.Exception: Baz")
                    true
                })
    }

    private class Filter(
        val positive: List<String>,
        val negative: List<String>,
        val expected: List<String>,
    ) {
        val args: List<String>
            get() = buildList {
                if (positive.isNotEmpty()) {
                    add("--ktest_gradle_filter=${positive.asListOfPatterns}")
                }
                if (negative.isNotEmpty()) {
                    add("--ktest_negative_gradle_filter=${negative.asListOfPatterns}")
                }
            }

        private val List<String>.asListOfPatterns: String
            get() = joinToString(",") { "kotlin.test.tests.$it" }
    }

    private fun doTestFilters(filter: Filter) {
        val regex = "Passed: (\\w+) \\(kotlin\\.test\\.tests\\.(\\w+)\\)".toRegex()
        fun MatchResult.toTestName(): String {
            val method = groups[1]!!.value
            val clazz = groups[2]!!.value
            return "${clazz}.${method}"
        }
        runTest("filters",
                listOf(testRoot.resolve("filters.kt")),
                executionArgs = listOf("--ktest_logger=SIMPLE") + filter.args,
                outputMatcher = TestRunCheck.OutputMatcher { output ->
                    val actual = output.lines().mapNotNull {
                        regex.matchEntire(it)?.toTestName()
                    }
                    assertContentEquals(filter.expected, actual)
                    true
                })
    }

    @Test
    fun testFiltersPositive() {
        doTestFilters(
            Filter(
                listOf("A.foo1", "B", "FiltersKt.foo1"), listOf(), listOf("A.foo1", "B.foo1", "B.foo2", "B.bar", "FiltersKt.foo1")
            )
        )
    }

    @Test
    fun testFiltersNegative() {
        doTestFilters(
            Filter(
                listOf(), listOf("A.foo1", "B", "FiltersKt.foo1"), listOf("A.foo2", "A.bar", "FiltersKt.foo2", "FiltersKt.bar")
            )
        )
    }

    @Test
    fun testFiltersPositiveNegative() {
        doTestFilters(
            Filter(
                listOf("A", "FiltersKt"), listOf("A.foo1", "FiltersKt.foo1"), listOf("A.foo2", "A.bar", "FiltersKt.foo2", "FiltersKt.bar")
            )
        )
    }

    @Test
    fun testFiltersPositiveGlob() {
        doTestFilters(Filter(listOf("A.foo*", "B.*"), listOf(), listOf("A.foo1", "A.foo2", "B.foo1", "B.foo2", "B.bar")))
    }

    @Test
    fun testFiltersNegativeGlob() {
        doTestFilters(Filter(listOf(), listOf("A.foo*", "B.*"), listOf("A.bar", "FiltersKt.foo1", "FiltersKt.foo2", "FiltersKt.bar")))
    }

    @Test
    fun testFiltersPositiveGlobNegative() {
        doTestFilters(Filter(listOf("*.foo*"), listOf("B"), listOf("A.foo1", "A.foo2", "FiltersKt.foo1", "FiltersKt.foo2")))
    }

    @Test
    fun testFiltersPositiveNegativeGlob() {
        doTestFilters(Filter(listOf("A"), listOf("*.foo*"), listOf("A.bar")))
    }

    private fun doTestFilteredSuites(filter: Filter) {
        val regex = "Hook: (.+)".toRegex()
        fun MatchResult.toHook(): String {
            return groups[1]!!.value
        }
        runTest("filteredSuites",
                listOf(testRoot.resolve("filtered_suites.kt")),
                executionArgs = listOf("--ktest_logger=SIMPLE") + filter.args,
                outputMatcher = TestRunCheck.OutputMatcher { output ->
                    val actual = output.lines().mapNotNull {
                        regex.matchEntire(it)?.toHook()
                    }
                    assertContentEquals(filter.expected, actual)
                    true
                })
    }

    @Test
    fun testFilteredSuitesTopLevel() {
        doTestFilteredSuites(Filter(listOf("Filtered_suitesKt.*"), listOf(), listOf("Filtered_suitesKt.before", "Filtered_suitesKt.after")))
    }

    @Test
    fun testFilteredSuitesClass() {
        doTestFilteredSuites(Filter(listOf("A.*"), listOf(), listOf("A.before", "A.after")))
    }

    @Test
    fun testFilteredSuitesAll() {
        doTestFilteredSuites(
            Filter(
                listOf("*.common"),
                listOf(),
                listOf("A.before", "A.after", "Filtered_suitesKt.before", "Filtered_suitesKt.after")
            )
        )
    }

    @Test
    fun testFilteredSuitesIgnoredClass() {
        doTestFilteredSuites(Filter(listOf("Ignored.*"), listOf(), listOf()))
    }

    @Test
    fun testFilteredSuitesIgnoredTest() {
        doTestFilteredSuites(Filter(listOf("A.ignored"), listOf(), listOf()))
    }
}

@Suppress("JUnitTestCaseWithNoTests")
@TestDataPath("\$PROJECT_ROOT")
class ClassicCompilerTestRunnerTest : CompilerTestRunnerTestBase()

@Suppress("JUnitTestCaseWithNoTests")
@FirPipeline
@Tag("frontend-fir")
@TestDataPath("\$PROJECT_ROOT")
class FirCompilerTestRunnerTest : CompilerTestRunnerTestBase()
