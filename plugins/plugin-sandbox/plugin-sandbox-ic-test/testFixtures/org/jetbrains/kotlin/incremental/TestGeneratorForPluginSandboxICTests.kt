/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.WITHOUT_UNDERSCORE_PREFIX

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(
            "plugins/plugin-sandbox/plugin-sandbox-ic-test/tests-gen",
            "plugins/plugin-sandbox/plugin-sandbox-ic-test/testData/jvmAndKlib"
        ) {
            testClass<AbstractIncrementalJvmWithPluginCompilerRunnerTest> {
                model("pureKotlin", extension = null, recursive = false)
            }
            testClass<AbstractIncrementalJvmWithPluginRelocatableCachesCompilerRunnerTest> {
                model("pureKotlin", extension = null, recursive = false)
            }
            testClass<AbstractIncrementalJsKlibWithPluginCompilerRunnerTest> {
                model("pureKotlin", extension = null, recursive = false)
            }
        }

        testGroup("plugins/plugin-sandbox/plugin-sandbox-ic-test/tests-gen", "plugins/plugin-sandbox/plugin-sandbox-ic-test/testData/js") {
            testClass<AbstractIncrementalCodegenJsWithPluginSandboxPerModuleTest> {
                model("pureKotlin", recursive = false, pattern = WITHOUT_UNDERSCORE_PREFIX)
            }
            testClass<AbstractIncrementalCodegenJsEs6WithPluginSandboxPerModuleTest> {
                model("pureKotlin", recursive = false, pattern = WITHOUT_UNDERSCORE_PREFIX)
            }
            testClass<AbstractIncrementalCodegenJsWithPluginSandboxPerFileTest> {
                model("pureKotlin", recursive = false, pattern = WITHOUT_UNDERSCORE_PREFIX)
            }
            testClass<AbstractIncrementalCodegenJsEs6WithPluginSandboxPerFileTest> {
                model("pureKotlin", recursive = false, pattern = WITHOUT_UNDERSCORE_PREFIX)
            }
        }
        testGroup("plugins/plugin-sandbox/plugin-sandbox-ic-test/tests-gen", "plugins/plugin-sandbox/plugin-sandbox-ic-test/testData/js") {
            testClass<AbstractIncrementalWasmWithPluginSandboxTest> {
                model("pureKotlin", recursive = false, pattern = WITHOUT_UNDERSCORE_PREFIX)
            }
            testClass<AbstractIncrementalWasmMultiModuleWithPluginSandboxTest> {
                model("pureKotlin", recursive = false, pattern = WITHOUT_UNDERSCORE_PREFIX)
            }
            testClass<AbstractIncrementalWasmSingleModuleWithPluginSandboxTest> {
                model("pureKotlin", recursive = false, pattern = WITHOUT_UNDERSCORE_PREFIX)
            }
        }
    }
}
