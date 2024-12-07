/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jspo

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlinx.jspo.runners.AbstractFirJsPlainObjectsIrJsBoxTest
import org.jetbrains.kotlinx.jspo.runners.AbstractFirJsPlainObjectsPluginDiagnosticTest

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(
            "plugins/js-plain-objects/compiler-plugin/tests-gen",
            "plugins/js-plain-objects/compiler-plugin/testData"
        ) {
            // ------------------------------- diagnostics -------------------------------
            testClass<AbstractFirJsPlainObjectsPluginDiagnosticTest>() {
                model("diagnostics")
            }

            // ------------------------------- box -------------------------------

            testClass<AbstractFirJsPlainObjectsIrJsBoxTest> {
                model("box")
            }
        }
    }
}
