/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeBlackBoxTest
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseExtTestCaseGroupProvider
import org.junit.jupiter.api.Tag

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        // LitmusKt tests.
        testGroup(testsRoot, "native/native.tests/litmus-tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "FirLitmusKtTestsGenerated",
                annotations = listOf(
                    litmusktNative(),
                    litmusktExecutionTimeout(),
                    provider<UseExtTestCaseGroupProvider>(),
                    forceHostTarget(),
                )
            ) {
                model("standalone")
            }
        }
    }
}

private fun litmusktNative() = annotation(Tag::class.java, "litmuskt-native")

/**
 * One test executable runs all test functions of a single test data file, and each of them spawns 8 worker
 * threads spinning on a barrier. On slow or oversubscribed CI agents this does not fit into the default
 * execution timeout. See KT-87923.
 */
private fun litmusktExecutionTimeout() = annotation(
    EnforcedProperty::class.java,
    "property" to ClassLevelProperty.EXECUTION_TIMEOUT,
    "propertyValue" to "20m"
)