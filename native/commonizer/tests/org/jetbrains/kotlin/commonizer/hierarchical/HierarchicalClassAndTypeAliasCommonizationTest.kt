/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized

class HierarchicalClassAndTypeAliasCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test commonization of typeAlias and class`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget("a", "typealias X = Int")
            simpleSingleSourceTarget("b", "class X")
        }

        result.assertCommonized("(a, b)", "expect class X")
        result.assertCommonized("a", "typealias X = Int")
        result.assertCommonized("b", "class X")
    }

    fun `test commonization of typeAlias and class hierarchically`() {
        val result = commonize {
            outputTarget("((a, b), (c, d))")
            simpleSingleSourceTarget("a", "typealias X = Int")
            simpleSingleSourceTarget("b", "typealias X = Long")
            simpleSingleSourceTarget("c", "class X")
            simpleSingleSourceTarget("d", "typealias X = Short")
        }

        result.assertCommonized("(a, b)", "expect class X")
        result.assertCommonized("(c, d)", "expect class X")
        result.assertCommonized("((a, b), (c, d))", "expect class X")
    }

    fun `test following typeAliases`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                typealias X = NotX

                class NotX(val constructorProperty: Int) {
                    val property: Int = 42
                    fun function() = 42
                }
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                class X(val constructorProperty: Int) {
                    val property: Int = 42
                    fun function() = 42
                }
            """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
            expect class X expect constructor(expect val constructorProperty: Int) {
                expect val property: Int
                expect fun function(): Int
            }
        """.trimIndent()
        )
    }


    fun `test following nested typeAliases`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                typealias X = NotX
                typealias NotX = ReallyNotX 
                class ReallyNotX(val constructorProperty: Int) {
                    val property: Int = 42
                    fun function() = 42
                }
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                class X(val constructorProperty: Int) {
                    val property: Int = 42
                    fun function() = 42
                }
            """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
            expect class X expect constructor(expect val constructorProperty: Int) {
                expect val property: Int
                expect fun function(): Int
            }
        """.trimIndent()
        )
    }
}
