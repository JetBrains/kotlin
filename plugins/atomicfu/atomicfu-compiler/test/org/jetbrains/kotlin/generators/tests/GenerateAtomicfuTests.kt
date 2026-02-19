/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit4.generateTestGroupSuiteWithJUnit4
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeCodegenBoxTest
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseExtTestCaseGroupProvider
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlinx.atomicfu.incremental.AbstractIncrementalK2JVMWithAtomicfuRunnerTest
import org.jetbrains.kotlinx.atomicfu.runners.*
import org.junit.jupiter.api.Tag

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit4(args) {
        testGroup("plugins/atomicfu/atomicfu-compiler/tests-gen", "plugins/atomicfu/atomicfu-compiler/testData/") {
            testClass<AbstractIncrementalK2JVMWithAtomicfuRunnerTest> {
                model("projects/", extension = null, recursive = false, targetBackend = TargetBackend.JVM_IR)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5 {
        // Atomicfu compiler plugin native tests.
        testGroup("plugins/atomicfu/atomicfu-compiler/tests-gen", "plugins/atomicfu/atomicfu-compiler/testData/box") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "AtomicfuNativeTestGenerated",
                annotations = listOf(*atomicfuNative(), provider<UseExtTestCaseGroupProvider>())
            ) {
                model()
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "AtomicfuNativeTestWithInlinedFunInKlibGenerated",
                annotations = listOf(klibIrInliner(), *atomicfuNative(), provider<UseExtTestCaseGroupProvider>())
            ) {
                model()
            }
        }

        testGroup(
            "plugins/atomicfu/atomicfu-compiler/tests-gen",
            "plugins/atomicfu/atomicfu-compiler/testData",
            testRunnerMethodName = "runTest0"
        ) {
            testClass<AbstractAtomicfuJsTest> {
                model("box/")
            }
            testClass<AbstractAtomicfuJsWithInlinedFunInKlibTest> {
                model("box/")
            }
        }

        testGroup(
            "plugins/atomicfu/atomicfu-compiler/tests-gen",
            "plugins/atomicfu/atomicfu-compiler/testData",
            testRunnerMethodName = "runTest0"
        ) {
            testClass<AbstractAtomicfuFirCheckerTest> {
                model("diagnostics/")
            }

            testClass<AbstractAtomicfuJvmIrTest> {
                model("box/", excludeDirs = listOf("context_parameters"))
            }

            testClass<AbstractAtomicfuJvmFirLightTreeTest> {
                model("box/")
            }
        }
    }
}

private fun atomicfuNative() = arrayOf(
    annotation(Tag::class.java, "atomicfu-native"),
    annotation(EnforcedHostTarget::class.java), // TODO(KT-65977): Make atomicfu tests run on all targets.
)
