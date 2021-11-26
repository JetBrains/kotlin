/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized

class ReturnTypeNullabilityCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test two nullable functions`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget("a", "fun x(): Any? = Unit")
            simpleSingleSourceTarget("b", "fun x(): Any? = Unit")
        }

        result.assertCommonized("(a, b)", "expect fun x(): Any?")
    }

    fun `test two non-nullable functions`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget("a", "fun x(): Any = Unit")
            simpleSingleSourceTarget("b", "fun x(): Any = Unit")
        }

        result.assertCommonized("(a, b)", "expect fun x(): Any")
    }

    fun `test nullable and non-nullable function`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget("a", "fun x(): Any? = null")
            simpleSingleSourceTarget("b", "fun x(): Any = null!!")
        }

        result.assertCommonized("(a, b)", "expect fun x(): Any?")
    }

    fun `test two nullable properties`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget("a", "val x: Any? = Unit")
            simpleSingleSourceTarget("b", "val x: Any? = Unit")
        }

        result.assertCommonized("(a, b)", "expect val x: Any?")
    }

    fun `test two non-nullable properties`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget("a", "val x: Any = Unit")
            simpleSingleSourceTarget("b", "val x: Any = Unit")
        }

        result.assertCommonized("(a, b)", "expect val x: Any")
    }

    fun `test nullable and non-nullable - property`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget("a", "val x: Any? = null")
            simpleSingleSourceTarget("b", "val x: Any = Unit")
        }

        result.assertCommonized("(a, b)", "expect val x: Any?")
    }

    fun `test nullable and non-nullable - var - val property`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget("a", "var x: Any? = null")
            simpleSingleSourceTarget("b", "val x: Any = Unit")
        }

        result.assertCommonized("(a, b)", "")
    }

    fun `test nullable and non-nullable - var var property`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget("a", "var x: Any? = null")
            simpleSingleSourceTarget("b", "var x: Any = Unit")
        }

        result.assertCommonized("(a, b)", "")
    }
    
    fun `test different nullability typealias  - function`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    typealias X = Any?
                    fun x(): X = null!!
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias X = Any
                    fun x(): X = null!!
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X
                expect fun x(): X?
            """.trimIndent()
        )
    }

    fun `test different nullability typealias chain - function`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    typealias Z = Any
                    typealias Y = Z?
                    typealias X = Y
                    fun x(): X = null!!
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias Z = Any
                    typealias Y = Z
                    typealias X = Y
                    fun x(): X = null!!
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias Z = Any
                expect class Y
                expect class X
                
                // Still cary nullability mark here to be extra safe
                expect fun x(): X?
            """.trimIndent()
        )
    }

    fun `test different nullability typealias chain - property`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    typealias Z = Any
                    typealias Y = Z?
                    typealias X = Y
                    val x: X = Unit
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias Z = Any
                    typealias Y = Z
                    typealias X = Y
                    val x: X = Unit
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias Z = Any
                expect class Y
                expect class X
                // Still cary nullability mark here to be more safe!
                expect val x: X?
            """.trimIndent()
        )
    }


    fun `test property - hierarchically`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")
            "a" withSource "val x: Any? = null"
            "b" withSource "val x: Any = Unit"
            "c" withSource "val x: Any = Unit"
            "d" withSource "val x: Any = Unit"
        }

        result.assertCommonized("(a, b)", "expect val x: Any?")
        result.assertCommonized("(c, d)", "expect val x: Any")
        result.assertCommonized("(a, b, c, d)", "expect val x: Any?")
    }


    /*
    We expect *no* covariant nullability commonization on any member function/property because this might mess
    with overrides of super classes/interfaces
     */

    fun `test member - property - hierarchically`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")

            simpleSingleSourceTarget(
                "a", """
                class X {
                    val x: Any? = Unit
                }
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                class X {
                    val x: Any = Unit
                }
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "c", """
                class X {
                    val x: Any = Unit
                }
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "d", """
                class X {
                    val x: Any = Unit
                }
            """.trimIndent()
            )
        }

        result.assertCommonized("(a, b)", """expect class X()""")
        result.assertCommonized("(c, d)", """expect class X() { val x: Any }""")
        result.assertCommonized("(a, b, c, d)", """expect class X()""")
    }

    fun `test member - function - hierarchically`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")

            simpleSingleSourceTarget(
                "a", """
                class X {
                    fun x(): Any? = null!!
                }
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                class X {
                    fun x(): Any = null!!
                }
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "c", """
                class X {
                   fun x(): Any = null!!
                }
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "d", """
                class X {
                    fun x(): Any = null!!
                }
            """.trimIndent()
            )
        }

        result.assertCommonized("(a, b)", """expect class X()""")
        result.assertCommonized("(c, d)", """expect class X() { fun x(): Any }""")
        result.assertCommonized("(a, b, c, d)", """expect class X()""")
    }
}