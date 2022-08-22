/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuite {
        testGroup(
            "plugins/kotlinx-serialization/tests-gen",
            "plugins/kotlinx-serialization/testData"
        ) {
            testClass<AbstractSerializationPluginDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractSerializationPluginBytecodeListingTest> {
                model("codegen")
            }

            testClass<AbstractSerializationIrBytecodeListingTest> {
                model("codegen")
            }
        }
    }

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(
            "plugins/kotlinx-serialization/tests-gen",
            "plugins/kotlinx-serialization/testData"
        ) {

            // New test infrastructure ONLY
            testClass<AbstractSerializationIrBoxTest> {
                model("boxIr")
            }

            testClass<AbstractSerializationWithoutRuntimeIrBoxTest> {
                model("boxWithoutRuntime")
            }

            testClass<AbstractSerializationFirMembersTest> {
                model("firMembers")
            }

            testClass<AbstractSerializationFirBlackBoxTest> {
                model("firMembers")
            }
        }
    }
}
