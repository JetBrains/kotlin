/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized


class ParameterizedTypesCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test simple parameterized class`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    class X<T, R>
                    fun<T, R> x1(x: X<T, R>)
                    fun <M> x2(x: X<M, String>)
                    fun x3(x: X<Int, String>)
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class X<T, R>
                    fun<T, R> x1(x: X<T, R>)
                    fun <M> x2(x: X<M, String>)
                    fun x3(x: X<Int, String>)
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X<T, R>()
                expect fun <T, R> x1(x: X<T, R>)
                expect fun <M> x2(x: X<M, String>)
                expect fun x3(x: X<Int, String>)
            """.trimIndent()
        )
    }

    fun `test parameterized type alias - 0`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    typealias TA1<T, R> = Map<T, R>
                    typealias TA2<R> = TA1<String, R>
                    typealias TA3 = TA2<Int>

                    fun x1(x: TA3)
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias TA1<T, R> = Map<T, R>
                    typealias TA2<R> = TA1<String, R>
                    typealias TA3 = TA2<Int>
                    
                    fun x1(x: TA3)
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias TA1<T, R> = Map<T, R>
                typealias TA2<R> = TA1<String, R>
                typealias TA3 = TA2<Int>
                
                expect fun x1(x: TA3)
            """.trimIndent()
        )
    }

    fun `test parameterized type alias - 1`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    typealias TA1<T, R> = Map<T, R>
                    typealias TA2<R> = TA1<String, R>
                    typealias TA3 = TA2<Int>

                    fun x1(x: TA3)
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias TA1<T, R> = Map<T, R>
                    typealias TA2<R> = TA1<String, R>
                    typealias TA3 = TA2<Int>
                    
                    fun x1(x: TA1<String, Int>)
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias TA1<T, R> = Map<T, R>
                typealias TA2<R> = TA1<String, R>
                typealias TA3 = TA2<Int>
                
                /*
                 Desirable solution below!
                 Test ensures that no wrong commonization is emitted for now.
                 */
                //expect fun x1(x: TA1<String, Int>) 
            """.trimIndent()
        )
    }

    fun `test parameterized type alias - 2`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class Triple<T, R, M>
                    typealias T1<T, R, M> = Triple<T, R, M>
                    typealias T2<T, R, M> = Triple<T, R, M>
                    typealias T3<T, M> = Triple<T, String, M>
                    typealias T4<T, M> = Triple<T, String, M>

                    fun x1(x: T4<Int, Long>)
                    fun x2(x: T3<Int, Long>)
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class Triple<T, R, M>
                    typealias T1<T, R, M> = Triple<T, R, M>
                    typealias T2<T, R, M> = Triple<T, R, M>
                    typealias T3<T, M> = Triple<T, String, M>
                    typealias T4<T, M> = Triple<T, String, M>

                    fun x1(x: T3<Int, Long>) // NOTE: T3 & T4 flipped
                    fun x2(x: T4<Int, Long>) // NOTE: T3 & T4 flipped
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                 expect class Triple<T, R, M>()
                 typealias T1<T, R, M> = Triple<T, R, M>
                 typealias T2<T, R, M> = Triple<T, R, M>
                 typealias T3<T, M> = Triple<T, String, M>
                 typealias T4<T, M> = Triple<T, String, M>
                 
                 /* Note: No hard requirement whether T4 or T3 is chosen! */
                 expect fun x1(x: T4<Int, Long>) 
                 expect fun x2(x: T4<Int, Long>)
            """.trimIndent()
        )
    }

    fun `test type alias from dependencies parameterized with library source type`() {
        val result = commonize {
            outputTarget("(a, b)")

            registerDependency("(a, b)") {
                source(
                    """
                    class Box<T>
                    typealias TA<T> = Box<T>
                    """.trimIndent()
                )
            }

            simpleSingleSourceTarget(
                "a", """
                    class MyClass
                    typealias X = TA<MyClass>
                    
                    fun x(x: X)
                    fun x2(x: TA<MyClass>)
                    fun x3(x: TA<X>)
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class MyClass
                    typealias X = TA<MyClass>
                    
                    fun x(x: X)
                    fun x2(x: TA<MyClass>)
                    fun x3(x: TA<X>)
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class MyClass()
                typealias X = TA<MyClass>
                
                expect fun x(x: X)
                expect fun x2(x: TA<MyClass>)
                expect fun x3(x: TA<X>)
            """.trimIndent()
        )
    }
}
