/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import kotlin.test.assertFails
import kotlin.test.assertFailsWith

class InlineSourceCommonizationHealthCheckTest : AbstractInlineSourcesCommonizationTest() {
    fun `test reference module with error diagnostics breaks tests`() {
        val result = commonize {
            outputTarget("(a, b)")
            "a" withSource "class X"
            "b" withSource "class X"
        }

        assertFails("Test should fail when source code of reference module contains errors") {
            result.assertCommonized("(a, b)", "expect class X() : kotlin.MissingSupertype")
        }
    }

    fun `test duplicated settings are forbidden`() {
        assertFailsWith<IllegalStateException>("Defining a setting multiple times should be forbidden") {
            commonize {
                outputTarget("(a, b)")
                setting(OptimisticNumberCommonizationEnabledKey, true)
                setting(OptimisticNumberCommonizationEnabledKey, false)
            }
        }
    }
}