/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("plugins/scripting/scripting-tests/tests-gen", "plugins/scripting/scripting-tests") {
            testClass<AbstractScriptWithCustomDefDiagnosticsTestBase> {
                model("testData/diagnostics/testScripts", extension = "kts")
            }

            testClass<AbstractScriptWithCustomDefBlackBoxCodegenTest> {
                model("testData/codegen/testScripts", extension = "kts")
            }

            testClass<AbstractReplWithTestExtensionsDiagnosticsTest> {
                model(
                    "testData/diagnostics/repl",
                    extension = "kts",
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                )
            }

            testClass<AbstractReplViaApiDiagnosticsTest> {
                model(
                    "testData/diagnostics/repl",
                    extension = "kts",
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                )
            }

            testClass<AbstractReplWithTestExtensionsCodegenTest> {
                model("testData/codegen/repl", extension = "kts")
            }

            testClass<AbstractReplViaApiEvaluationTest> {
                model("testData/codegen/repl", extension = "kts")
            }
        }
    }
}
