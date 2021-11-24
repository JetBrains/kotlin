/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.group

import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.WithTestRunnerExtras
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCaseGroup
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCaseGroupId
import org.jetbrains.kotlin.konan.blackboxtest.support.TestFile
import org.jetbrains.kotlin.konan.blackboxtest.support.TestModule
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ThreadSafeFactory
import org.jetbrains.kotlin.konan.blackboxtest.support.util.expandGlobTo
import org.jetbrains.kotlin.konan.blackboxtest.support.util.getAbsoluteFile
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.junit.jupiter.api.fail
import java.io.File

internal class PredefinedTestCaseGroupProvider(annotation: PredefinedTestCases) : TestCaseGroupProvider {
    private val testCaseIdToPredefinedTestCase: Map<TestCaseId.Named, PredefinedTestCase> = buildMap {
        annotation.testCases.forEach { predefinedTestCase ->
            val testCaseId = TestCaseId.Named(predefinedTestCase.name)
            if (put(testCaseId, predefinedTestCase) != null)
                fail { "Duplicated test cases found: $testCaseId" }

        }
    }

    // Assumption: Every test case group contains exactly one test case.
    private val testCaseGroupIdToTestCaseId: Map<TestCaseGroupId.Named, TestCaseId.Named> = buildMap {
        testCaseIdToPredefinedTestCase.keys.forEach { testCaseId ->
            val testCaseGroupId = testCaseId.testCaseGroupId
            if (put(testCaseGroupId, testCaseId) != null)
                fail { "Duplicated test case groups found: $testCaseGroupId" }
        }
    }

    private val lazyTestCaseGroups = ThreadSafeFactory<TestCaseGroupId.Named, TestCaseGroup?> { testCaseGroupId ->
        val testCaseId = testCaseGroupIdToTestCaseId[testCaseGroupId] ?: return@ThreadSafeFactory null
        val predefinedTestCase = testCaseIdToPredefinedTestCase[testCaseId] ?: return@ThreadSafeFactory null

        val module = TestModule.Exclusive(
            name = testCaseId.uniqueName,
            directDependencySymbols = emptySet(),
            directFriendSymbols = emptySet()
        )

        predefinedTestCase.sourceLocations
            .expandGlobs { "No files found for test case $testCaseId" }
            .forEach { file -> module.files += TestFile.createCommitted(file, module) }

        val testCase = TestCase(
            id = testCaseId,
            kind = TestKind.STANDALONE,
            modules = setOf(module),
            freeCompilerArgs = predefinedTestCase.freeCompilerArgs
                .parseCompilerArgs { "Failed to parse free compiler arguments for test case $testCaseId" },
            nominalPackageName = testCaseId.uniqueName,
            expectedOutputDataFile = null,
            extras = WithTestRunnerExtras
        )
        testCase.initialize(null)

        TestCaseGroup.Default(disabledTestCaseIds = emptySet(), testCases = listOf(testCase))
    }

    override fun setPreprocessors(testDataDir: File, preprocessors: List<(String) -> String>) {
        // do nothing
    }

    override fun getTestCaseGroup(testCaseGroupId: TestCaseGroupId): TestCaseGroup? {
        assertTrue(testCaseGroupId is TestCaseGroupId.Named)
        return lazyTestCaseGroups[testCaseGroupId.cast()]
    }

    companion object {
        private fun Array<String>.expandGlobs(noExpandedFilesErrorMessage: () -> String): Set<File> {
            val files = buildSet {
                this@expandGlobs.forEach { pathPattern -> expandGlobTo(getAbsoluteFile(pathPattern), this) }
            }
            assertTrue(files.isNotEmpty(), noExpandedFilesErrorMessage)
            return files
        }

        private fun Array<String>.parseCompilerArgs(parsingErrorMessage: () -> String): TestCompilerArgs =
            if (isEmpty())
                TestCompilerArgs.EMPTY
            else {
                val freeCompilerArgs = asList()
                val forbiddenCompilerArgs = TestCompilerArgs.findForbiddenArgs(freeCompilerArgs)
                assertTrue(forbiddenCompilerArgs.isEmpty()) {
                    """
                        ${parsingErrorMessage()}
                        
                        Forbidden compiler arguments found: $forbiddenCompilerArgs
                        All arguments: $this
                    """.trimIndent()
                }

                TestCompilerArgs(freeCompilerArgs)
            }
    }
}
