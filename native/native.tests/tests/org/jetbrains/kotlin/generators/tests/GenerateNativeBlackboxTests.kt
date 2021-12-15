/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.blackboxtest.AbstractExternalNativeBlackBoxTest
import org.jetbrains.kotlin.konan.blackboxtest.AbstractNativeBlackBoxTest
import org.jetbrains.kotlin.konan.blackboxtest.support.group.UseExtTestCaseGroupProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.group.UseStandardTestCaseGroupProvider
import org.jetbrains.kotlin.test.TargetBackend
import org.junit.jupiter.api.Tag

fun main() {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit5 {
        // External box tests.
        testGroup("native/native.tests/tests-gen", "compiler/testData") {
            testClass<AbstractExternalNativeBlackBoxTest>(
                suiteTestClassName = "ExternalTestGenerated",
                annotations = listOf(external(), provider<UseExtTestCaseGroupProvider>())
            ) {
                model("codegen/box", targetBackend = TargetBackend.NATIVE)
                model("codegen/boxInline", targetBackend = TargetBackend.NATIVE)
            }
        }

        // Samples (how to utilize the abilities of new test infrastructure).
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "InfrastructureTestGenerated",
                annotations = listOf(infrastructure(), provider<UseStandardTestCaseGroupProvider>())
            ) {
                model("samples")
                model("samples2")
            }
        }
    }
}

private inline fun <reified T : Annotation> provider() = annotation(T::class.java)

private fun external() = annotation(Tag::class.java, "external")
private fun infrastructure() = annotation(Tag::class.java, "infrastructure")
