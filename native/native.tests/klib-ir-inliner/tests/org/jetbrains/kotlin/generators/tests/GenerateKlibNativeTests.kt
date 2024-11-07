/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.inlining.AbstractNativeUnboundIrSerializationTest
import org.junit.jupiter.api.Tag

fun main() {
    System.setProperty("java.awt.headless", "true")
    val k1BoxTestDir = listOf("multiplatform/k1")

    generateTestGroupSuiteWithJUnit5 {
        // New frontend test infrastructure tests
        testGroup(testsRoot = "native/native.tests/klib-ir-inliner/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractNativeUnboundIrSerializationTest>(
                suiteTestClassName = "NativeUnboundIrSerializationTestGenerated",
                annotations = listOf(*frontendFir(), klib())
            ) {
                /*
                 * Note: "all-files" tests are consciously not generated to have a more clear picture of test coverage:
                 * - Some test data files don't have inline functions. There is basically nothing to test in them.
                 *   So, such tests end up in "ignored" (gray) state.
                 * - The tests that fail are "failed" (red).
                 * - Successful tests (with really processed inline functions) are "successful" (green).
                 */
                model("codegen/box", skipTestAllFilesCheck = true, excludeDirs = k1BoxTestDir)
                model("codegen/boxInline", skipTestAllFilesCheck = true)
            }
        }
    }
}

fun frontendFir() = arrayOf(
    annotation(Tag::class.java, "frontend-fir"),
    annotation(FirPipeline::class.java)
)

private fun klib() = annotation(Tag::class.java, "klib")
