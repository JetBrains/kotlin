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

    fun `test follow typeAlias on both platforms`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                class A {
                    val x: Int = 42
                }
                typealias X = A
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                class B {
                    val x: Int = 42
                }
                typealias X = B
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X expect constructor() {
                    expect val x: Int
                }
            """.trimIndent()
        )
    }

    fun `test follow exact same typeAlias on both platforms`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                class AB {
                    val x: Int = 42
                }
                typealias X = AB
            """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                class AB {
                    val x: Int = 42
                }
                typealias X = AB
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class AB expect constructor() {
                    expect val x: Int
                }
                typealias X = AB
            """.trimIndent()
        )
    }

    fun `test aliased class with companion`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    class A {
                        companion object
                    }
                    
                    typealias X = A
                """.trimIndent()
            )
            simpleSingleSourceTarget(
                "b", """
                    class X {
                        companion object
                    }
                """.trimIndent()
            )
        }

        result.assertCommonized("(a, b)", """expect class X expect constructor()""")
    }

    fun `test typeAlias with nullability`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    class A
                    typealias X = A?
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class B
                    typealias X = B
                """.trimIndent()
            )
        }

        result.assertCommonized("(a, b)", """expect class X""")
    }

    fun `test typeAlias chain with nullability`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    class AB
                    typealias V = AB?
                    typealias Y = V
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class AB
                    typealias V = AB
                    typealias Y = V
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class AB expect constructor()
                expect class V
                typealias Y = V
            """.trimIndent()
        )
    }

    fun `test typeAlias chain with nullability 2`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    class AB
                    typealias V = AB
                    typealias Y = V?
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class AB
                    typealias V = AB
                    typealias Y = V
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class AB expect constructor()
                expect typealias V = AB
                expect class Y
            """.trimIndent()
        )
    }
}
