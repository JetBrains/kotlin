/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeCodegenBoxTest
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseExtTestCaseGroupProvider
import org.junit.jupiter.api.Tag

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    val k1BoxTestDir = listOf("multiplatform/k1")

    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        // Codegen box tests.
        testGroup(testsRoot, "compiler/testData/codegen") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "FirNativeCodegenBoxTestGenerated",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                    codegenBox(),
                )
            ) {
                model("box", excludeDirs = k1BoxTestDir)
                model("boxInline")
            }
        }
    }
}

private fun codegenBox() = annotation(Tag::class.java, "codegen-box")
