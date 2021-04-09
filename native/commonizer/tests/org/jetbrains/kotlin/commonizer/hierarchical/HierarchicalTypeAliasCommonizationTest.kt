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
            outputTarget("((a,b), (c,d))")
            simpleSingleSourceTarget("a", "typealias X = Int")
            simpleSingleSourceTarget("b", "typealias X = Int")
            simpleSingleSourceTarget("c", "typealias X = Int")
            simpleSingleSourceTarget("d", "typealias X = Int")
        }

        result.assertCommonized("((a,b), (c,d))", "typealias X = Int")
        result.assertCommonized("(a,b)", "typealias X = Int")
        result.assertCommonized("(c, d)", "typealias X = Int")

        /* Special case: For now, leaves should depend on commonized platform libraries */
        result.assertCommonized("a", "")
        result.assertCommonized("b", "")
        result.assertCommonized("c", "")
        result.assertCommonized("d", "")
    }

    /**
     * See: https://youtrack.jetbrains.com/issue/KT-45992
     */
    fun `todo test typealias and class`() {
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

        result.assertCommonized("(a,b)", "expect class X")
    }

    fun `test typealias to different classes`() {
        val result = commonize {
            outputTarget("(((a,b), (c,d)), (e,f))")
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

        result.assertCommonized("a", """class AB""")
        result.assertCommonized("b", """class AB""")
        result.assertCommonized("c", """class CD""")
        result.assertCommonized("d", """class CD""")
        result.assertCommonized("e", """class x""")
        result.assertCommonized("f", """class x""")

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

        result.assertCommonized("((a,b), (c,d))", """expect class x""")
        result.assertCommonized("(((a,b), (c,d)), (e,f))", """expect class x""")
    }
}
