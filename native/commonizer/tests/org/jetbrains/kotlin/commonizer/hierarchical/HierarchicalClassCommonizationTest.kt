/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized

class HierarchicalClassCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test simple class`() {
        val result = commonize {
            outputTarget("((a,b), (c,d), e)")
            simpleSingleSourceTarget("a", "class X")
            simpleSingleSourceTarget("b", "class X")
            simpleSingleSourceTarget("c", "class X")
            simpleSingleSourceTarget("d", "class X")
            simpleSingleSourceTarget("e", "class X")
        }

        result.assertCommonized("a", "class X")
        result.assertCommonized("b", "class X")
        result.assertCommonized("c", "class X")
        result.assertCommonized("d", "class X")
        result.assertCommonized("e", "class X")

        result.assertCommonized("(a,b)", "expect class X expect constructor()")
        result.assertCommonized("(c,d)", "expect class X expect constructor()")
        result.assertCommonized("(a,b)", "expect class X expect constructor()")
        result.assertCommonized("((a,b), (c,d), e)", "expect class X expect constructor()")
    }

    fun `test sample class`() {
        val result = commonize {
            outputTarget("((a,b), (c,d))")
            simpleSingleSourceTarget(
                "a", """
                   class X {
                        val a: Int = 42
                        val ab: Int = 42
                        val abcd: Int = 42
                   } 
                """
            )

            simpleSingleSourceTarget(
                "b", """
                   class X {
                        val b: Int = 42
                        val ab: Int = 42
                        val abcd: Int = 42
                   } 
                """
            )

            simpleSingleSourceTarget(
                "c", """
                   class X {
                        val c: Int = 42
                        val cd: Int = 42
                        val abcd: Int = 42
                   } 
                """
            )

            simpleSingleSourceTarget(
                "d", """
                   class X {
                        val d: Int = 42
                        val cd: Int = 42
                        val abcd: Int = 42
                   } 
                """
            )
        }

        result.assertCommonized(
            "a", """
               class X {
                    val a: Int = 42
                    val ab: Int = 42
                    val abcd: Int = 42
               } 
                """
        )

        result.assertCommonized(
            "b", """
               class X {
                    val b: Int = 42
                    val ab: Int = 42
                    val abcd: Int = 42
               } 
                """
        )

        result.assertCommonized(
            "c", """
               class X {
                    val c: Int = 42
                    val cd: Int = 42
                    val abcd: Int = 42
               } 
                """
        )

        result.assertCommonized(
            "d", """
               class X {
                   val d: Int = 42
                   val cd: Int = 42
                   val abcd: Int = 42
               } 
                """
        )

        result.assertCommonized(
            "(a,b)", """
               expect class X expect constructor() {
                    expect val ab: Int
                    expect val abcd: Int
               } 
                """
        )

        result.assertCommonized(
            "(c,d)", """
               expect class X expect constructor() {
                    expect val cd: Int
                    expect val abcd: Int
               } 
                """
        )

        result.assertCommonized(
            "((a,b), (c,d))", """
               expect class X expect constructor() {
                    expect val abcd: Int
               } 
                """
        )
    }
}
