/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.konan.diagnostics.AbstractDiagnosticsNativeTest
import org.jetbrains.kotlin.konan.diagnostics.AbstractFirLightTreeNativeDiagnosticsTest
import org.jetbrains.kotlin.konan.diagnostics.AbstractFirPsiNativeDiagnosticsTest
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

fun main() {
    System.setProperty("java.awt.headless", "true")
    val excludedCustomTestdataPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN

    generateTestGroupSuiteWithJUnit5 {
        // New frontend test infrastructure tests
        testGroup(testsRoot = "native/dignostic-tests/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractDiagnosticsNativeTest> {
                model("diagnostics/nativeTests", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirPsiNativeDiagnosticsTest>(
                suiteTestClassName = "FirPsiOldFrontendNativeDiagnosticsTestGenerated",
            ) {
                model("diagnostics/nativeTests", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirLightTreeNativeDiagnosticsTest>(
                suiteTestClassName = "FirLightTreeOldFrontendNativeDiagnosticsTestGenerated",
            ) {
                model("diagnostics/nativeTests", excludedPattern = excludedCustomTestdataPattern)
            }
        }
    }
}
