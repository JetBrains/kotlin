/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized

class FunctionReturnTypeCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test non-commonized return type of top level function`() {
        val result = commonize {
            outputTarget("(a,b)")
            simpleSingleSourceTarget(
                "a", """
                class A { // NOTE: Class
                    class B
                }
                fun x(): A.B = TODO()
            """
            )

            simpleSingleSourceTarget(
                "b", """
                interface A { // NOTE: Interface
                    class B
                }
                fun x(): A.B = TODO()
                """
            )
        }

        /**
         * -> A is not commonized (interface vs class)
         * -> B is not commonized
         * -> fun x can't be commonized
         * -> Empty commonization
         */
        result.assertCommonized(
            "(a,b)", ""
        )

        result.assertCommonized(
            "a", """
            class A {
                class B
            }
            fun x(): A.B = TODO()
            """
        )

        result.assertCommonized(
            "b", """
            interface A {
                class B
            }
            fun x(): A.B = TODO()
            """
        )
    }


    fun `test commonized return type of top level function`() {
        val result = commonize {
            outputTarget("(a,b)")
            simpleSingleSourceTarget(
                "a", """
                interface A { 
                    class B
                }
                fun x(): A.B = TODO()
            """
            )

            simpleSingleSourceTarget(
                "b", """
                interface A { 
                    class B
                }
                fun x(): A.B = TODO()
                """
            )
        }

        /**
         * -> A is not commonized (interface vs class)
         * -> B is not commonized
         * -> fun x can't be commonized
         * -> Empty commonization
         */
        result.assertCommonized(
            "(a,b)", """
                expect interface A { 
                    expect class B expect constructor()
                }
                expect fun x(): A.B
            """
        )

        result.assertCommonized(
            "a", """
                interface A {
                    class B
                }
                fun x(): A.B = TODO()
            """
        )

        result.assertCommonized(
            "b", """
                interface A {
                    class B
                }
                fun x(): A.B = TODO()
            """
        )
    }
}
