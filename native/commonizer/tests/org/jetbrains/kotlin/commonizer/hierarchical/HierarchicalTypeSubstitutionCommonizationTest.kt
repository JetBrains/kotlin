/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized

class HierarchicalTypeSubstitutionCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test function with boxed parameter`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class Box<T>
                    class A
                    typealias X = A
                    fun x(x: Box<X>) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class Box<T>
                    class B
                    typealias X = B
                    fun x(x: Box<B>) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class Box<T>()
                expect class X()
                expect fun x(x: Box<X>)
            """.trimIndent()
        )
    }


    fun `test function with boxed parameter - with box from dependencies`() {
        val result = commonize {
            outputTarget("(a, b)")

            registerDependency("a", "b", "(a, b)") {
                source("""class Box<T>""")
            }

            simpleSingleSourceTarget(
                "a", """
                    class A
                    typealias X = A
                    fun x(x: Box<X>) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class B
                    typealias X = B
                    fun x(x: Box<B>) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X()
                expect fun x(x: Box<X>)
            """.trimIndent()
        )
    }

    fun `test function parameter with suitable typealias`() {
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
                expect class X()
                expect fun useX(x: X)
            """.trimIndent()
        )
    }

    fun `test boxed property return type`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class Box<T>
                    class A
                    typealias X = A
                    val x: Box<X> = null!!
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class Box<T>
                    class B
                    typealias X = B
                    val x: Box<B> = null!!
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class Box<T>()
                expect class X()
                expect val x: Box<X>
            """.trimIndent()
        )
    }
}