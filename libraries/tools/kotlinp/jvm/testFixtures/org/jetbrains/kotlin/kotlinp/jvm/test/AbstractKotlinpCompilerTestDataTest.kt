/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.jvm.test

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB

abstract class AbstractKotlinpCompilerTestDataTest : AbstractKotlinpTest() {

    // It would be excessive to dump kotlinp-read declarations for the compiler loadJava test data in addition to the already existing
    // dumps. Instead, we're only checking that the dump is the same after the write-read transformation, just to verify that
    override val compareWithTxt: Boolean
        get() = false

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)

        with(builder) {
            forTestsMatching("compiler/testData/loadJava/compiledKotlinWithStdlib/*") {
                defaultDirectives {
                    +WITH_STDLIB
                }
            }
        }
    }
}
