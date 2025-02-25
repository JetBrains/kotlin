/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeCodegenBoxTest
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseExtTestCaseGroupProvider
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlinx.atomicfu.incremental.AbstractIncrementalK2JVMWithAtomicfuRunnerTest
import org.jetbrains.kotlinx.atomicfu.runners.AbstractAtomicfuFirCheckerTest
import org.jetbrains.kotlinx.atomicfu.runners.AbstractAtomicfuJsFirTest
import org.jetbrains.kotlinx.atomicfu.runners.AbstractAtomicfuJsIrTest
import org.jetbrains.kotlinx.atomicfu.runners.AbstractAtomicfuJvmFirLightTreeTest
import org.jetbrains.kotlinx.atomicfu.runners.AbstractAtomicfuJvmIrTest
import org.jetbrains.kotlinx.atomicfu.runners.AbstractAtomicfuNativeIrTextTest
import org.junit.jupiter.api.Tag

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuite(args) {
        testGroup("plugins/atomicfu/atomicfu-compiler/tests-gen", "plugins/atomicfu/atomicfu-compiler/testData/") {
            testClass<AbstractIncrementalK2JVMWithAtomicfuRunnerTest> {
                model("projects/", extension = null, recursive = false, targetBackend = TargetBackend.JVM_IR)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5 {
        testGroup("plugins/atomicfu/atomicfu-compiler/tests-gen", "plugins/atomicfu/atomicfu-compiler/testData/box") {
            testClass<AbstractAtomicfuNativeIrTextTest>(
                suiteTestClassName = "AtomicfuNativeIrTextTestGenerated",
                annotations = listOf(*atomicfuNative(), *frontendFir(), provider<UseExtTestCaseGroupProvider>())
            ) {
                model()
            }
        }

        // Atomicfu compiler plugin native tests.
        testGroup("plugins/atomicfu/atomicfu-compiler/tests-gen", "plugins/atomicfu/atomicfu-compiler/testData/box") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "AtomicfuNativeTestGenerated",
                annotations = listOf(*atomicfuNative(), *frontendClassic(), provider<UseExtTestCaseGroupProvider>())
            ) {
                model(targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "AtomicfuNativeFirTestGenerated",
                annotations = listOf(*atomicfuNative(), *frontendFir(), provider<UseExtTestCaseGroupProvider>())
            ) {
                model(targetBackend = TargetBackend.NATIVE)
            }
        }

        testGroup(
            "plugins/atomicfu/atomicfu-compiler/tests-gen",
            "plugins/atomicfu/atomicfu-compiler/testData",
            testRunnerMethodName = "runTest0"
        ) {
            testClass<AbstractAtomicfuJsIrTest> {
                model("box/")
            }

            testClass<AbstractAtomicfuJsFirTest> {
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
                model("box/")
            }

            testClass<AbstractAtomicfuJvmFirLightTreeTest> {
                model("box/")
            }
        }
    }
}

private fun atomicfuNative() = arrayOf(
    annotation(Tag::class.java, "atomicfu-native"),
)
