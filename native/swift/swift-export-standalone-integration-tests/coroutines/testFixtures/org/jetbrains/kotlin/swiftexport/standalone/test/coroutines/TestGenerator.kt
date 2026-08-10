/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test.coroutines

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.generators.tests.provider
import org.jetbrains.kotlin.konan.target.KonanTarget.MACOS_ARM64
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseExtTestCaseGroupProvider
import org.jetbrains.kotlin.swiftexport.standalone.test.AbstractSwiftExportExecutionTest
import org.jetbrains.kotlin.swiftexport.standalone.test.AbstractSwiftExportWithBinaryCompilationTest
import org.jetbrains.kotlin.swiftexport.standalone.test.AbstractSwiftExportWithResultValidationTest
import org.jetbrains.kotlin.swiftexport.standalone.test.EnabledOnNativeTargets
import org.jetbrains.kotlin.swiftexport.standalone.test.SwiftExportWithCoroutinesTestSupport
import org.junit.jupiter.api.extension.ExtendWith

fun main(args: Array<String>) {
    generateCoroutinesSuite(args)
}

fun generateCoroutinesSuite(args: Array<String>, classNamePrefix: String = "") {
    val testsRoot = args[0]
    // Coroutines/atomicfu test klibs are prebuilt for macos_arm64 only on the CI agents.
    val targetRestrictionAnnotation = annotation(EnabledOnNativeTargets::class.java, "targets" to arrayOf(MACOS_ARM64.name))

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "native/swift/swift-export-standalone-integration-tests/coroutines/testData/generation") {
            testClass<AbstractSwiftExportWithResultValidationTest>(
                suiteTestClassName = "${classNamePrefix}SwiftExportCoroutinesWithResultValidationTest",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                    annotation(ExtendWith::class.java, SwiftExportWithCoroutinesTestSupport::class.java),
                    targetRestrictionAnnotation,
                ),
            ) {
                model("", extension = null, recursive = false)
            }
        }
        testGroup(testsRoot, "native/swift/swift-export-standalone-integration-tests/coroutines/testData/generation") {
            testClass<AbstractSwiftExportWithBinaryCompilationTest>(
                suiteTestClassName = "${classNamePrefix}SwiftExportCoroutinesWithBinaryCompilationTest",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                    annotation(ExtendWith::class.java, SwiftExportWithCoroutinesTestSupport::class.java),
                    targetRestrictionAnnotation,
                ),
            ) {
                model("", extension = null, recursive = false)
            }
        }
        testGroup(testsRoot, "native/swift/swift-export-standalone-integration-tests/coroutines/testData/execution") {
            testClass<AbstractSwiftExportExecutionTest>(
                suiteTestClassName = "${classNamePrefix}SwiftExportCoroutinesExecutionTestGenerated",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                    annotation(ExtendWith::class.java, SwiftExportWithCoroutinesTestSupport::class.java),
                    targetRestrictionAnnotation,
                ),
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
        }
    }
}
