/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual

import org.jetbrains.kotlin.generators.tests.generator.testGroup

fun main(args: Array<String>) {
    testGroup("plugins/contracts/contracts-subplugins/test", "plugins/contracts/contracts-subplugins/testData") {
        testClass<AbstractContextualEffectsDiagnosticTest> {
            model("contracts")
        }

        testClass<AbstractLoadJavaContractsTest> {
            model("loadJava", extension = "kt", testMethod = "doTestCompiledKotlinWithStdlib")
        }
    }
}