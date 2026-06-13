/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test.simple

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.tests.provider
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseExtTestCaseGroupProvider
import org.jetbrains.kotlin.swiftexport.standalone.test.AbstractSwiftExportExecutionTest
import org.jetbrains.kotlin.swiftexport.standalone.test.AbstractSwiftExportWithBinaryCompilationTest
import org.jetbrains.kotlin.swiftexport.standalone.test.AbstractSwiftExportWithResultValidationTest

fun main(args: Array<String>) {
    generateSimpleSuite(args)
}

fun generateSimpleSuite(args: Array<String>, classNamePrefix: String = "") {
    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "native/swift/swift-export-standalone-integration-tests/simple/testData/generation") {
            testClass<AbstractSwiftExportWithResultValidationTest>(
                suiteTestClassName = "${classNamePrefix}SwiftExportWithResultValidationTest",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                ),
            ) {
                model("", extension = null, recursive = false)
            }
        }
        testGroup(testsRoot, "native/swift/swift-export-standalone-integration-tests/simple/testData/generation") {
            testClass<AbstractSwiftExportWithBinaryCompilationTest>(
                suiteTestClassName = "${classNamePrefix}SwiftExportWithBinaryCompilationTest",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                ),
            ) {
                model("", extension = null, recursive = false)
            }
        }
        testGroup(testsRoot, "native/swift/swift-export-standalone-integration-tests/simple/testData/execution") {
            testClass<AbstractSwiftExportExecutionTest>(
                suiteTestClassName = "${classNamePrefix}SwiftExportExecutionTestGenerated",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                ),
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
        }
    }
}
