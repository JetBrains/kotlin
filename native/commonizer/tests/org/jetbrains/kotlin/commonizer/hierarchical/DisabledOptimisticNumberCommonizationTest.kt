/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.OptimisticNumberCommonizationEnabledKey
import org.jetbrains.kotlin.commonizer.assertCommonized

class DisabledOptimisticNumberCommonizationTest : AbstractInlineSourcesCommonizationTest() {
    fun `test non-platform types`() {
        val result = commonize {
            outputTarget("(a, b)")
            setting(OptimisticNumberCommonizationEnabledKey, false)
            registerFakeStdlibIntegersDependency("(a, b)")

            "a" withSource """
                typealias X = Short
            """.trimIndent()

            "b" withSource """
                typealias X = Int
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X : Number
            """.trimIndent()
        )
    }

    fun `test platform types`() {
        val result = commonize {
            outputTarget("(a, b)")
            setting(OptimisticNumberCommonizationEnabledKey, false)
            registerFakeStdlibIntegersDependency("(a, b)")

            "a" withSource """
                typealias X = Int
            """.trimIndent()

            "b" withSource """
                typealias X = Long
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X : Number
            """.trimIndent()
        )
    }
}
