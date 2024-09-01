/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.FrontendConfiguratorTestGenerator

fun main() {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit5(additionalMethodGenerators = listOf(FrontendConfiguratorTestGenerator)) {
        testGroup("plugins/compose/compiler-hosted/tests-gen", "plugins/compose/compiler-hosted/testData") {
            testClass<AbstractCompilerFacilityTestForComposeCompilerPlugin> {
                model("codegen")
            }
        }
    }
}
