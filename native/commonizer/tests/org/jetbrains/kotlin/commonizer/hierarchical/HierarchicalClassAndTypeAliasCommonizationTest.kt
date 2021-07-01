/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized
import org.junit.Test

class HierarchicalClassAndTypeAliasCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test commonization of typeAlias and class`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget("a", "typealias X = Int")
            simpleSingleSourceTarget("b", "class X")
        }

        result.assertCommonized("(a, b)", "expect class X")
    }

    fun `test commonization of class and typeAlias`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget("a", "class X")
            simpleSingleSourceTarget("b", "typealias X = Int")
        }

        result.assertCommonized("(a, b)", "expect class X")
    }

    fun `test commonization of typeAlias and class hierarchically`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")
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
                expect class Y
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

    fun `test return types`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class X 
                    fun createX(): X
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class B
                    typealias X = B
                    fun createX(): X
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X expect constructor()
                expect fun createX(): X
            """.trimIndent()
        )
    }

    fun `test function parameters`() {
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
                    class B
                    typealias X = B
                    fun useX(x: X) = Unit
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

    fun `test parameterized return type`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")
            registerDependency("a", "b", "c", "d", "(a, b)", "(c, d)", "(a, b, c, d)") {
                source("class Box<T>")
            }
            simpleSingleSourceTarget(
                "a", """
                    class X
                    fun createBox(): Box<X>
                """.trimIndent()
            )
            simpleSingleSourceTarget(
                "b", """
                    class B
                    typealias X = B
                    fun createBox(): Box<X>
                """.trimIndent()
            )
            simpleSingleSourceTarget(
                "c", """
                    class CD
                    typealias X = CD
                    fun createBox(): Box<X>
                """.trimIndent()
            )
            simpleSingleSourceTarget(
                "d", """
                    class CD
                    typealias X = CD
                    fun createBox(): Box<X>
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X expect constructor()
                expect fun createBox(): Box<X>
            """.trimIndent()
        )

        result.assertCommonized(
            "(c, d)", """
                expect class CD expect constructor()
                typealias X = CD
                expect fun createBox(): Box<X>
            """.trimIndent()
        )

        result.assertCommonized(
            "(a, b, c, d)", """
                expect class X expect constructor()
                expect fun createBox(): Box<X>
            """.trimIndent()
        )
    }

    fun `test boxed parameter in function`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class Box<T>
                    class X
                    fun useBox(x: Box<X>)
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class Box<T>
                    class B 
                    typealias X = B
                    fun useBox(x: Box<X>)
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class Box<T> expect constructor()
                expect class X expect constructor()
                expect fun useBox(x: Box<X>)
            """.trimIndent()
        )
    }

    fun `test boxed parameter in function - TA expansion not commonized`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class Box<T>
                    class A
                    typealias X = A
                    fun useBox(x: Box<X>)
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class Box<T>
                    class B 
                    typealias X = B
                    fun useBox(x: Box<X>)
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class Box<T> expect constructor()
                expect class X expect constructor()
                expect fun useBox(x: Box<X>)
            """.trimIndent()
        )
    }

    fun `test supertype from dependency`() {
        val result = commonize {
            outputTarget("(a, b)")
            registerDependency("a", "b", "(a, b)") {
                source("interface SuperClass")
            }

            simpleSingleSourceTarget(
                "a", """
                    class A: SuperClass
                    typealias X = A
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class X: SuperClass
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X expect constructor(): SuperClass
            """.trimIndent()
        )
    }

    fun `test supertype from sources`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    interface SuperClass
                    class A: SuperClass
                    typealias X = A
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    interface SuperClass
                    class X: SuperClass
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect interface SuperClass
                expect class X expect constructor(): SuperClass
            """.trimIndent()
        )
    }

    fun `test typealias to numbers`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")

            simpleSingleSourceTarget(
                "a", """
                    typealias Proxy = Long
                    typealias X = Proxy
                    const val x: X = 42L
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias Proxy = Long
                    typealias X = Proxy
                    const val x: X = 42L
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "c", """
                    typealias Proxy = Int
                    typealias X = Proxy
                    const val x: X = 42
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "d", """
                    typealias Proxy = Short
                    typealias X = Proxy
                    const val x: X = 42
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias Proxy = Long
                typealias X = Proxy
                const val x: X = 42L
            """.trimIndent()
        )

        result.assertCommonized(
            "(c, d)", """
                expect class Proxy
                typealias X = Proxy
                expect val x: X
            """.trimIndent()
        )

        result.assertCommonized(
            "(a, b, c, d)", """
                expect class Proxy
                typealias X = Proxy
                expect val x: X
            """.trimIndent()
        )
    }

    @Suppress("unused")
    fun `ignored KT-47433 - test boxed function using TA and expanded type`() {
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

    @Suppress("unused")
    fun `ignored KT-47433 - test parameters with non-commonized TA expanding to a commonized type`() {
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
