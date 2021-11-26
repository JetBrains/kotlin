/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.transformer

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized

class UnderscoredTypeAliasTypeSubstitutonTest : AbstractInlineSourcesCommonizationTest() {

    fun `test inlined underscored typealias - single platform`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    typealias X = Int
                    typealias __X = X
                    fun x(x: __X) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias X = Int
                    fun x(x: X) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias X = Int
                expect fun x(x: X)
            """.trimIndent()
        )
    }

    fun `test inlined underscored typealias - both platforms`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    typealias X = Int
                    typealias __X = X
                    fun x(x: __X) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias X = Int
                    typealias __X = X
                    fun x(x: X) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias X = Int
                typealias __X = X
                /* No hard requirement. Picking X over __X seems equally fine here */
                expect fun x(x: X)
            """.trimIndent()
        )
    }

    fun `test inlined underscored typealias - both platforms - not used in signature`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    typealias X = Int
                    typealias __X = X
                    fun x(x: X) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias X = Int
                    typealias __X = X
                    fun x(x: X) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias X = Int
                typealias __X = X
                expect fun x(x: X)
            """.trimIndent()
        )
    }


    fun `test inlined underscored typealias - both platforms - underscore used in both signatures`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    typealias X = Int
                    typealias __X = X
                    fun x(x: __X) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias X = Int
                    typealias __X = X
                    fun x(x: __X) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias X = Int
                typealias __X = X
                expect fun x(x: __X)
            """.trimIndent()
        )
    }
}