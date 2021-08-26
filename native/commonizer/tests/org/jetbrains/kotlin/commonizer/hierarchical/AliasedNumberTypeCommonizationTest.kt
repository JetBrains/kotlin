/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized
import org.jetbrains.kotlin.commonizer.utils.InlineSourceBuilder

class AliasedNumberTypeCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test Int and Long - typealias`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget("a", "typealias X = Int")
            simpleSingleSourceTarget("b", "typealias X = Long")
        }

        result.assertCommonized("(a, b)", "typealias X = Long")
    }

    fun `test UInt and ULong - typealias`() {
        val result = commonize {
            outputTarget("(a, b)")
            registerDependency("a", "b", "(a, b)") { unsignedIntegers() }
            simpleSingleSourceTarget("a", "typealias X = UInt")
            simpleSingleSourceTarget("b", "typealias X = ULong")
        }

        result.assertCommonized("(a, b)", "typealias X = ULong")
    }


    fun `test int and long - chain - typealias`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    typealias A = Int
                    typealias B = A
                    typealias C = B
                    typealias X = C
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias A = Long
                    typealias B = A
                    typealias X = B
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias A = Long
                typealias B = A
                typealias X = Long
            """.trimIndent()
        )
    }

    fun `test function with pure number types parameter`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget("a", "fun x(p: Int)")
            simpleSingleSourceTarget("b", "fun x(p: Long)")
        }

        /*
        Only functions that use a TA in their signature are supposed to be
        commonized using our number's commonization hack.

        This is a hard requirement. It would also be reasonable if we would add
        support for this case, since there would be reasonable code that people
        could write with this!
         */
        result.assertCommonized("(a, b)", "")
    }

    fun `test function with aliased number value parameter`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    typealias A = Int
                    typealias X = A
                    fun x(p: X)
                """.trimIndent()
            )
            simpleSingleSourceTarget(
                "b", """
                    typealias B = Long
                    typealias X = B
                    fun x(p: X)
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias X = Long
                expect fun x(p: X)
            """.trimIndent()
        )
    }

    fun `test property with pure number return type`() {
        val result = commonize {
            outputTarget("(a, b)")
            registerDependency("a", "b", "(a, b)") { unsignedIntegers() }
            simpleSingleSourceTarget("a", "val x: UInt")
            simpleSingleSourceTarget("b", "val x: ULong")
        }

        /*
        Only commonize return types that were specified using a type-alias.
        As with function value parameters, this is not a hard requirement.
        It would also be reasonable to support this case.
         */
        result.assertCommonized("(a, b)", "")
    }

    fun `test property with aliased number return type`() {
        val result = commonize {
            outputTarget("(a, b)")
            registerDependency("a", "b", "(a, b)") { unsignedIntegers() }
            simpleSingleSourceTarget(
                "a", """
                    typealias X = UShort
                    val x: X = TODO()
                """.trimIndent()
            )
            simpleSingleSourceTarget(
                "b", """
                    typealias X = ULong
                    val x: X = TODO()
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias X = ULong
                expect val x: X 
            """.trimIndent()
        )
    }
}

private fun InlineSourceBuilder.ModuleBuilder.unsignedIntegers() {
    source(
        """
        package kotlin
        class UByte
        class UShort
        class UInt
        class ULong
        """.trimIndent(), "unsigned.kt"
    )
}