/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test

import org.jetbrains.kotlin.generators.dsl.junit4.generateTestGroupSuiteWithJUnit4
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.parcelize.test.runners.AbstractParcelizeBytecodeListingTest
import org.jetbrains.kotlin.parcelize.test.runners.AbstractParcelizeBoxTest
import org.jetbrains.kotlin.parcelize.test.runners.AbstractParcelizeDiagnosticTest
import org.jetbrains.kotlin.parcelize.test.runners.AbstractParcelizeIncrementalTest
import org.jetbrains.kotlin.test.TargetBackend

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit4(args) {
        testGroup(args[0], "plugins/parcelize/parcelize-compiler/testData") {
            testClass<AbstractParcelizeIncrementalTest> {
                model("incremental", extension = null, recursive = false, targetBackend = TargetBackend.JVM_IR)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(args[0], "plugins/parcelize/parcelize-compiler/testData") {
            testClass<AbstractParcelizeBoxTest> {
                model("box")
            }

            testClass<AbstractParcelizeBytecodeListingTest> {
                model("codegen")
            }

            testClass<AbstractParcelizeDiagnosticTest> {
                model("diagnostics", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
        }
    }
}
