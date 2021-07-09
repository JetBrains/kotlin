/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized

class HierarchicalTypeSubstitutionCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test boxed function using TA and expanded type`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class Box<T>
                    class A
                    typealias X = A
                    fun x(x: Box<X>)
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class Box<T>
                    class B
                    typealias X = B
                    fun x(x: Box<B>)
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class Box<T> expect constructor()
                expect class X expect constructor()
                expect fun x(x: Box<X>)
            """.trimIndent()
        )
    }


    fun `test boxed function using TA and expanded type - with box from dependencies`() {
        val result = commonize {
            outputTarget("(a, b)")

            registerDependency("a", "b", "(a, b)") {
                source("""class Box<T>""")
            }

            simpleSingleSourceTarget(
                "a", """
                    class A
                    typealias X = A
                    fun x(x: Box<X>)
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class B
                    typealias X = B
                    fun x(x: Box<B>)
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X expect constructor()
                expect fun x(x: Box<X>)
            """.trimIndent()
        )
    }

    fun `test parameters with non-commonized TA expanding to a commonized type`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class X 
                    fun useX(x: X) = Unit
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class X
                    typealias B = X
                    fun useX(x: B) = Unit
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X expect constructor()
                expect fun useX(x: X)
            """.trimIndent()
        )
    }
}