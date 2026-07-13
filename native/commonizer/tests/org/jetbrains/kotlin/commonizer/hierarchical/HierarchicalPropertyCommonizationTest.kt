/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized
import org.junit.jupiter.api.Test

class HierarchicalPropertyCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    @Test
    fun `test simple property`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")
            simpleSingleSourceTarget("a", "val x: Int = 42")
            simpleSingleSourceTarget("b", "val x: Int = 42")
            simpleSingleSourceTarget("c", "val x: Int = 42")
            simpleSingleSourceTarget("d", "val x: Int = 42")
        }

        result.assertCommonized("((a,b), (c,d))", "expect val x: Int")
        result.assertCommonized("(a, b)", "expect val x: Int")
        result.assertCommonized("(c, d)", "expect val x: Int")
    }

    @Test
    fun `test same typeAliased property`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                     typealias TA = Int
                     val x: TA = 42
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias TA = Int
                    val x: TA = 42
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
            typealias TA = Int
            expect val x: TA
        """.trimIndent()
        )
    }

    @Test
    fun `test differently typeAliased property - expanded type from dependencies`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                     typealias TA_A = Int
                     val x: TA_A = 42
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias TA_B = Int
                    val x: TA_B = 42
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
            expect val x: Int
        """.trimIndent()
        )
    }

    @Test
    fun `test differently typeAliased property - expanded type from sources`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    class AB
                    typealias TA_A = AB
                    val x: TA_A = TA_A()
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class AB
                    typealias TA_B = AB
                    val x: TA_B = TA_B()
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class AB()
                expect val x: AB
        """.trimIndent()
        )
    }

    @Test
    fun `test typeAliased property and class typed property`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    class AB
                    typealias TA = AB
                    val x: TA = TA()
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class AB
                    val x: AB = AB()
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class AB()
                expect val x: AB
        """.trimIndent()
        )
    }

    @Test
    fun `test class typed property and typeAliased property`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    class AB
                    val x: AB = AB()
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class AB
                    typealias TA = AB
                    val x: TA = TA()
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class AB()
                expect val x: AB
        """.trimIndent()
        )
    }


    @Test
    fun `test single typeAliased property and double typeAliased property`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    class AB
                    typealias TA_AB = AB
                    val x: TA_AB = TA_AB()
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class AB
                    typealias TA_AB = AB
                    typealias TA_B = TA_AB
                    val x: TA_B = TA_B()
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class AB()
                typealias TA_AB = AB
                expect val x: TA_AB
        """.trimIndent()
        )
    }

    @Test
    fun `test single typeAliased property and double typeAliased property - with reversed order`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    class AB
                    typealias TA_AB = AB
                    typealias TA_B = TA_AB
                    val x: TA_B = TA_B()
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class AB
                    typealias TA_AB = AB
                    val x: TA_AB = TA_AB()
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class AB()
                typealias TA_AB = AB
                expect val x: TA_AB
        """.trimIndent()
        )
    }

    @Test
    fun `test property with and without setter`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    val x: Int = 42
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    var x: Int = 42
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect var x: Int
                    private set
            """.trimIndent()
        )
    }

    @Test
    fun `test property with annotations`() {
        val result = commonize {
            outputTarget("(a, b)")
            registerDependency("a", "b", "(a, b)") {
                source("""
                    annotation class A1
                    annotation class A2
                    annotation class A3
                """.trimIndent())
            }

            simpleSingleSourceTarget("a", """@A1 @A2 @A3 val x: Int = 42""")
            simpleSingleSourceTarget("b", """@A1 @A2 val x: Int = 42""")
        }

        result.assertCommonized("(a, b)", """@A1 @A2 expect val x: Int""")
    }
}
