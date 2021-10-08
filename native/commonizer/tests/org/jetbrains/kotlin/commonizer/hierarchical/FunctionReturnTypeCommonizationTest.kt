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
                fun x(): A.B = null!!
            """
            )

            simpleSingleSourceTarget(
                "b", """
                interface A { // NOTE: Interface
                    class B
                }
                fun x(): A.B = null!!
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
    }


    fun `test commonized return type of top level function`() {
        val result = commonize {
            outputTarget("(a,b)")
            simpleSingleSourceTarget(
                "a", """
                interface A { 
                    class B
                }
                fun x(): A.B = null!!
            """
            )

            simpleSingleSourceTarget(
                "b", """
                interface A { 
                    class B
                }
                fun x(): A.B = null!!
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
                    class B()
                }
                expect fun x(): A.B
            """
        )
    }
}
