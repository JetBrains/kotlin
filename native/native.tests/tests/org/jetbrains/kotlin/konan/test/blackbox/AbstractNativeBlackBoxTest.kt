/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import junit.framework.AssertionFailedError
import org.jetbrains.kotlin.cli.common.arguments.allowTestsOnlyLanguageFeatures
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeBlackBoxTestSupport
import org.jetbrains.kotlin.konan.test.blackbox.support.PackageName
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseId
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.group.isIgnoredTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRun
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunners.createProperTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CompatibilityTestMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TreeNode
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.konan.test.blackbox.support.util.joinPackageNames
import org.jetbrains.kotlin.konan.test.blackbox.support.util.prependPackageName
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(NativeBlackBoxTestSupport::class)
abstract class AbstractNativeBlackBoxTest {
    lateinit var testRunSettings: TestRunSettings
    lateinit var testRunProvider: TestRunProvider

    /**
     * Run JUnit test.
     *
     * This function should be called from a method annotated with [org.junit.jupiter.api.Test].
     */
    open fun runTest(@TestDataFile testDataFilePath: String) {
        allowTestsOnlyLanguageFeatures()
        val absoluteTestFile = getAbsoluteFile(testDataFilePath)
        val testCaseId = TestCaseId.TestDataFile(absoluteTestFile)
        try {
            runTestCase(testCaseId)
        } catch (e: CompilationToolException) {
            if (testRunSettings.get<CompatibilityTestMode>().isBackward) {
                if (((e.failure.loggedData as? LoggedData.CompilationToolCall)?.input as? LoggedData.CompilerInput)?.isFirstPhase == true) {
                    // 1st phase of klib backward testing may fail, since old compiler not necessarily can compile newer code
                    return
                }
            }
            // TODO find out the way not to re-read test source file, but to re-use already extracted test directives.
            if (testRunSettings.isIgnoredTarget(absoluteTestFile))
                println("There was an expected failure: CompilationToolException: ${e.reason}")
            else
                fail { e.reason }
        }
    }

    /**
     * Run JUnit test.
     *
     * This function should be called from a method annotated with [org.junit.jupiter.api.Test].
     */
    internal fun runTestCase(testCaseId: TestCaseId) {
        val testRun = testRunProvider.getSingleTestRun(testCaseId, testRunSettings)
        performTestRun(testRun)
    }

    /**
     * Run JUnit dynamic test.
     *
     * This function should be called from a method annotated with [org.junit.jupiter.api.TestFactory].
     */
    fun dynamicTest(@TestDataFile testDataFilePath: String): Collection<DynamicNode> {
        val testCaseId = TestCaseId.TestDataFile(getAbsoluteFile(testDataFilePath))
        return dynamicTestCase(testCaseId)
    }

    /**
     * Run JUnit dynamic test.
     *
     * This function should be called from a method annotated with [org.junit.jupiter.api.TestFactory].
     */
    internal fun dynamicTestCase(testCaseId: TestCaseId): Collection<DynamicNode> {
        val testRunNodes = testRunProvider.getTestRuns(testCaseId, testRunSettings)
        return buildJUnitDynamicNodes(testRunNodes)
    }

    // We have to use planar (one-level) tree of JUnit5 dynamic nodes, because Gradle does not support yet rendering
    // tests with arbitrary nesting level in their test reports. As long as these reports are consumed by various CI (such as TeamCity)
    // we have almost no chance to display test results in CI properly.
    private fun buildJUnitDynamicNodes(testRunNodes: Collection<TreeNode<TestRun>>): Collection<DynamicNode> =
    // This is the proper implementation that should be used instead:
//        buildList {
//            testRunNodes.forEach { testRunNode ->
//                testRunNode.items.mapTo(this) { testRun ->
//                    dynamicTest(testRun.displayName) { runTest(testRun) }
//                }
//
//                testRunNode.children.mapTo(this) { childTestRunNode ->
//                    dynamicContainer(childTestRunNode.packageSegment, buildJUnitDynamicNodes(childTestRunNode))
//                }
//            }
//        }
        buildList {
            fun TreeNode<TestRun>.processItems(parentPackageSegment: PackageName) {
                val ownPackageSegment = joinPackageNames(parentPackageSegment, packageSegment)
                items.mapTo(this@buildList) { testRun ->
                    val displayName = testRun.displayName.prependPackageName(ownPackageSegment)
                    dynamicTest(displayName) { performTestRun(testRun) }
                }

                children.forEach { it.processItems(ownPackageSegment) }
            }

            testRunNodes.forEach { testRunNode -> testRunNode.processItems(PackageName.EMPTY) }
        }

    private fun performTestRun(testRun: TestRun) {
        val testRunner = createProperTestRunner(testRun, testRunSettings)
        try {
            testRunner.run()
        } catch (e: AssertionError) {
            val compatibilityTestMode = testRunSettings.get<CompatibilityTestMode>()
            if (compatibilityTestMode.isBackward) {
                throw AssertionFailedError("Klib Backward Compatibility Error: current compiler fails compiling klib made by an older compiler.\n" +
                            "Should this test be a regression test, feel free to ignore it with \n" +
                            "// IGNORE_NATIVE: compatibilityTestMode=${compatibilityTestMode.name}\n" +
                            "Otherwise, please consult with Common Backend team regarding possible backwared compatibility issue:\n" +
                            "${e.message}")
            }
            throw e
        }
    }
}
