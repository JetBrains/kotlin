/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone.test.simple

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.tests.provider
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseExtTestCaseGroupProvider
import org.jetbrains.kotlin.swiftimport.standalone.test.AbstractSwiftImportExecutionTest
import org.jetbrains.kotlin.swiftimport.standalone.test.AbstractSwiftImportWithResultValidationTest

fun main(args: Array<String>) {
    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "native/swift/swift-import-standalone-integration-tests/simple/testData/generation") {
            testClass<AbstractSwiftImportWithResultValidationTest>(
                suiteTestClassName = "SwiftImportWithResultValidationTest",
                annotations = listOf(
                    // Native test settings require a test configuration annotation.
                    provider<UseExtTestCaseGroupProvider>(),
                ),
            ) {
                model("", extension = null, recursive = false)
            }
        }

        testGroup(testsRoot, "native/swift/swift-import-standalone-integration-tests/simple/testData/execution") {
            testClass<AbstractSwiftImportExecutionTest>(
                suiteTestClassName = "SwiftImportExecutionTestGenerated",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                ),
            ) {
                model("", extension = null, recursive = false)
            }
        }
    }
}
