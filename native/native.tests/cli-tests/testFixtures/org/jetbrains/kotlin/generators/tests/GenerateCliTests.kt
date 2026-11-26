/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.incremental.ExcludePattern
import org.jetbrains.kotlin.konan.test.cli.AbstractKlibToolCliTest
import org.jetbrains.kotlin.konan.test.cli.AbstractNativeCliTest
import org.jetbrains.kotlin.konan.test.klib.ic.AbstractIncrementalNativeKlibCompilerRunnerTest
import org.jetbrains.kotlin.konan.test.klib.ic.AbstractIncrementalNativeKlibMultiModuleCompilerRunnerTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    val mainClassName = TestGeneratorUtil.getMainClassName()

    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args, mainClassName) {
        testGroup(testsRoot, "native/native.tests/cli-tests/testData") {
            testClass<AbstractNativeCliTest> {
                model("cli", extension = "args", testMethod = "doNativeTest", recursive = false)
            }
            testClass<AbstractKlibToolCliTest> {
                model("cli/klib-tool", extension = "args", testMethod = "doKlibToolTest", recursive = false)
            }
        }

        // Copied from JS analog in `TestGeneratorForICTests`
        testGroup(testsRoot, "jps/jps-plugin/testData") {
            testClass<AbstractIncrementalNativeKlibCompilerRunnerTest> {
                // IC of sealed interfaces are not supported in JS
                modelForDirectoryBasedTest(
                    "incremental", "pureKotlin", extension = null, recursive = false,
                    // TODO: 'fileWithConstantRemoved' should be fixed in https://youtrack.jetbrains.com/issue/KT-58824
                    excludedPattern = listOf(
                        "^(sealed.*|fileWithConstantRemoved|propertyRedeclaration|funRedeclaration|funVsConstructorOverloadConflict|bridgeWithThrows)",
                        "(^changeTypealiasTypeWithHierarchy$)", // KT-89851: the klib IC cache does not track typealiases
                        "(^changeTopLevelTypeAlias$)", // KT-89851: the klib IC cache does not track typealiases
                        ExcludePattern.JPS_ONLY,
                    ).joinToString("|")
                )
                modelForDirectoryBasedTest(
                    "incremental", "classHierarchyAffected", extension = null, recursive = false,
                    excludedPattern = listOf("secondaryConstructorAdded", ExcludePattern.JPS_ONLY).joinToString("|")
                )
                modelForDirectoryBasedTest(
                    "incremental", "js", extension = null, excludeParentDirs = true,
                    excludedPattern = ExcludePattern.JPS_ONLY
                )
            }

            testClass<AbstractIncrementalNativeKlibMultiModuleCompilerRunnerTest> {
                modelForDirectoryBasedTest("incremental/multiModule", "common", extension = null, excludeParentDirs = true)
            }
        }
    }
}
