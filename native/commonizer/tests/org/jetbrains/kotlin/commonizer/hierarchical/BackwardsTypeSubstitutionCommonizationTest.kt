/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized

class BackwardsTypeSubstitutionCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test sample 0`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class A
                    typealias A1 = A
                    typealias X = A1
                    typealias Y = X
                    typealias Z = Y
                
                    fun x(x: A) {}
                    fun x1(x: A1) {}
                    fun x2(x: X) {}
                """.trimIndent()
            )


            simpleSingleSourceTarget(
                "b", """
                    class B
                    typealias B1 = B
                    typealias X = B1
                    typealias Y = X
                    typealias Z = Y
                    
                    fun x(x: B) {}
                    fun x1(x: B1) {}
                    fun x2(x: X) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X()
                typealias Y = X
                typealias Z = Y
                
                expect fun x(x: X)
                expect fun x1(x: X)
                expect fun x2(x: X)
            """.trimIndent()
        )
    }

    fun `test sample 1`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class A
                    typealias A1 = A
                    typealias X = A1
                    typealias Y = X
                    typealias Z = Y
                
                    fun x(x: A) {}
                    fun x1(x: A1) {}
                    fun x2(x: X) {}
                """.trimIndent()
            )


            simpleSingleSourceTarget(
                "b", """
                    class B
                    typealias B1 = B
                    typealias X = B1
                    typealias Y = X
                    typealias Z = Y
                    
                    fun x(x: Z) {}
                    fun x1(x: Z) {}
                    fun x2(x: Z) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X()
                typealias Y = X
                typealias Z = Y
                
                expect fun x(x: X)
                expect fun x1(x: X)
                expect fun x2(x: X)
            """.trimIndent()
        )
    }
}