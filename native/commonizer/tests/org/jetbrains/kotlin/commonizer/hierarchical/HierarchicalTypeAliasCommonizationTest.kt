/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized

class HierarchicalTypeAliasCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test simple type alias`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")
            simpleSingleSourceTarget("a", "typealias X = Int")
            simpleSingleSourceTarget("b", "typealias X = Int")
            simpleSingleSourceTarget("c", "typealias X = Int")
            simpleSingleSourceTarget("d", "typealias X = Int")
        }

        result.assertCommonized("((a,b), (c,d))", "typealias X = Int")
        result.assertCommonized("(a,b)", "typealias X = Int")
        result.assertCommonized("(c, d)", "typealias X = Int")
    }

    /**
     * See: https://youtrack.jetbrains.com/issue/KT-45992
     */
    fun `test typealias and class`() {
        val result = commonize {
            outputTarget("(a,b)")
            simpleSingleSourceTarget("a", """class X """)
            simpleSingleSourceTarget(
                "b", """
                class B
                typealias X = B
                """
            )
        }

        result.assertCommonized("(a,b)", "expect class X expect constructor()")
    }

    fun `test typealias to different classes`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(e, f)", "(a, b, c, d)", "(a, b, c, d, e, f)")
            simpleSingleSourceTarget(
                "a", """
                class AB
                typealias x = AB
            """
            )
            simpleSingleSourceTarget(
                "b", """
                class AB
                typealias x = AB
            """
            )
            simpleSingleSourceTarget(
                "c", """
                class CD
                typealias x = CD
            """
            )
            simpleSingleSourceTarget(
                "d", """
                class CD
                typealias x = CD
            """
            )
            simpleSingleSourceTarget("e", """class x""")
            simpleSingleSourceTarget("f", """class x""")
        }

        result.assertCommonized(
            "(a,b)", """
                expect class AB expect constructor()
                typealias x = AB
            """
        )

        result.assertCommonized(
            "(c,d)", """
                expect class CD expect constructor()
                typealias x = CD
            """
        )

        result.assertCommonized(
            "(c,d)", """
                expect class CD expect constructor()
                typealias x = CD
            """
        )

        result.assertCommonized(
            "(e,f)", """expect class x expect constructor()"""
        )

        result.assertCommonized("(a, b, c, d)", """expect class x expect constructor()""")
        result.assertCommonized("(a, b, c, d, e, f)", """expect class x expect constructor()""")
    }


    fun `test typealias chain`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    typealias A<N, M> = Map<N, M>
                    typealias B = A<String, Int>
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias A<N, M> = Map<N, M>
                    typealias B = A<String, Int>
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias A<N, M> = Map<N, M>
                typealias B = A<String, Int>
            """.trimIndent()
        )
    }

    fun `KT-47574 - test long typealias chain`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    typealias A<N, M> = Map<N, M>
                    typealias B<N, M> = A<N, M>
                    typealias C<N> = B<N, Int>
                    typealias D = C<String>
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias A<N, M> = Map<N, M>
                    typealias B<N, M> = A<N, M>
                    typealias C<N> = B<N, Int>
                    typealias D = C<String>
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias A<N, M> = Map<N, M>
                typealias B<N, M> = A<N, M>
                typealias C<N> = B<N, Int>
                typealias D = C<String>
            """.trimIndent()
        )
    }
}
