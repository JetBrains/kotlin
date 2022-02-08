/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized
import org.junit.Test

class ClassSuperTypeCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    @Test
    fun `test common supertype - from dependencies`() {
        val result = commonize {
            outputTarget("(a, b)")
            registerDependency("a", "b", "(a, b)") {
                source(
                    """
                    interface A
                    interface B: A
                """.trimIndent()
                )
            }

            simpleSingleSourceTarget(
                "a", """
                    class X: A
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class X: B
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X(): A
            """.trimIndent()
        )
    }

    @Test
    fun `test common supertype - from sources`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    interface A
                    interface B: A
                    class X: A
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    interface A
                    interface B: A
                    class X: B
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect interface A
                expect interface B: A
                expect class X(): A
            """.trimIndent()
        )
    }

    @Test
    fun `test common supertype - from sources - missing interface in one target`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    interface A
                    class X: A
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    interface A
                    interface B: A
                    class X: B
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect interface A
                expect class X(): A
            """.trimIndent()
        )
    }

    @Test
    fun `test common supertypes - sample 0`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    interface C
                    interface B
                    interface A: B, C
                    
                    class X: A
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    interface C
                    interface B: C
                    interface A
                    
                    class X: A, B
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect interface C
                expect interface B
                expect interface A
                
                expect class X(): A, B, C
            """.trimIndent()
        )
    }

    @Test
    fun `test common supertypes - sample 1`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    interface C
                    open class B
                    open class A: C, B()
                    
                    class X: A()
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    interface C
                    interface B: C
                    open class A
                    
                    class X: A(), B
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect interface C
                expect open class A()
                expect class X(): A, C
            """.trimIndent()
        )
    }

    @Test
    fun `test common supertypes - sample 2`() {
        val result = commonize {
            outputTarget("(a, b, c)")
            simpleSingleSourceTarget(
                "a", """
                    interface C
                    interface B
                    interface A: B, C
                
                    class X: A
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    interface C
                    interface B
                    interface A: B
            
                    class X: A, C
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "c", """
                    interface C
                    interface B: C
                    interface A
            
                    class X: A, B
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b, c)", """
                expect interface C
                expect interface B
                expect interface A
                
                expect class X(): A, B, C
            """.trimIndent()
        )
    }
}