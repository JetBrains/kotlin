/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.blackboxtest.support.NativeBlackBoxTestSupport
import org.jetbrains.kotlin.konan.blackboxtest.support.PackageName
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCaseId
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRun
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunners.createProperTestRunner
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.TestRunSettings
import org.jetbrains.kotlin.konan.blackboxtest.support.util.TreeNode
import org.jetbrains.kotlin.konan.blackboxtest.support.util.getAbsoluteFile
import org.jetbrains.kotlin.konan.blackboxtest.support.util.joinPackageNames
import org.jetbrains.kotlin.konan.blackboxtest.support.util.prependPackageName
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(NativeBlackBoxTestSupport::class)
abstract class AbstractNativeBlackBoxTest {
    internal lateinit var testRunSettings: TestRunSettings
    internal lateinit var testRunProvider: TestRunProvider

    /**
     * Run JUnit test.
     *
     * This function should be called from a method annotated with [org.junit.jupiter.api.Test].
     */
    fun runTest(@TestDataFile testDataFilePath: String) {
        val testCaseId = TestCaseId.TestDataFile(getAbsoluteFile(testDataFilePath))
        runTestCase(testCaseId)
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
        testRunner.run()
    }
}
