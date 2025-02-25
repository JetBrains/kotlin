/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseExtTestCaseGroupProvider
import org.jetbrains.kotlinx.atomicfu.runners.AbstractAtomicfuNativeIrTextTest
import org.junit.jupiter.api.Tag

fun main() {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit5 {
        testGroup("plugins/atomicfu/atomicfu-compiler/tests-gen", "plugins/atomicfu/atomicfu-compiler/testData/box") {
            testClass<AbstractAtomicfuNativeIrTextTest>(
                suiteTestClassName = "AtomicfuNativeIrTextTestGenerated",
                annotations = listOf(*atomicfuNative(), *frontendFir(), provider<UseExtTestCaseGroupProvider>())
            ) {
                model()
            }
        }
    }
}


fun frontendFir() = arrayOf(
    annotation(FirPipeline::class.java)
)

private fun atomicfuNative() = arrayOf(
    annotation(Tag::class.java, "atomicfu-native"),
//    annotation(EnforcedHostTarget::class.java), // TODO(KT-65977): Make atomicfu tests run on all targets.
)
