/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.test.TargetBackend

fun main(args: Array<String>) {
    generateTestGroupSuite(args) {
        testGroup("plugins/jvm-abi-gen/tests-gen", "plugins/jvm-abi-gen/testData") {
            testClass<AbstractCompareJvmAbiTest> {
                model("compare", recursive = false, extension = null, targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractJvmAbiContentTest> {
                model("content", recursive = false, extension = null, targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractCompileAgainstJvmAbiTest> {
                model("compile", recursive = false, extension = null, targetBackend = TargetBackend.JVM_IR)
            }
        }
    }
}
