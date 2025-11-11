/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeBlackBoxTest
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseStandardTestCaseGroupProvider
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
                    provider<UseStandardTestCaseGroupProvider>(),
                    forceHostTarget(),
                )
            ) {
                model("standalone")
            }
        }
    }
}

private fun litmusktNative() = annotation(Tag::class.java, "litmuskt-native")