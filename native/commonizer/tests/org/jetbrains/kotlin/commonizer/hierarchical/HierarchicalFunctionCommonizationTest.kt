/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized

class HierarchicalFunctionCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test simple function 1`() {
        val result = commonize {
            outputTarget("((a,b), (c,d))")
            simpleSingleSourceTarget("a", "fun x(): Int = 42")
            simpleSingleSourceTarget("b", "fun x(): Int = 42")
            simpleSingleSourceTarget("c", "fun x(): Int = 42")
            simpleSingleSourceTarget("d", "fun x(): Int = 42")
        }

        result.assertCommonized("((a,b), (c,d))", "expect fun x(): Int")
        result.assertCommonized("(a,b)", "expect fun x(): Int")
        result.assertCommonized("(c,d)", "expect fun x(): Int")
        result.assertCommonized("a", "actual fun x(): Int = 42")
        result.assertCommonized("b", "actual fun x(): Int = 42")
        result.assertCommonized("c", "actual fun x(): Int = 42")
        result.assertCommonized("d", "actual fun x(): Int = 42")
    }

    fun `test simple function 2`() {
        val result = commonize {
            outputTarget("((a,b), c)")
            simpleSingleSourceTarget("a", "fun x(): Int = 42")
            simpleSingleSourceTarget("b", "fun x(): Int = 42")
            simpleSingleSourceTarget("c", "fun x(): Int = 42")
        }

        result.assertCommonized("((a,b), c)", "expect fun x(): Int")
        result.assertCommonized("(a,b)", "expect fun x(): Int")
        result.assertCommonized("a", "fun x(): Int = 42")
        result.assertCommonized("b", "fun x(): Int = 42")
        result.assertCommonized("c", "fun x(): Int = 42")
    }

    fun `test function with returnType`() {
        val result = commonize {
            outputTarget("((a,b), (c,d))")
            simpleSingleSourceTarget(
                "a", """
                interface ABCD
                fun x(): ABCD = TODO()
            """
            )
            simpleSingleSourceTarget(
                "b", """
                interface ABCD
                fun x(): ABCD = TODO()
                """
            )
            simpleSingleSourceTarget(
                "c", """
                interface ABCD
                fun x(): ABCD = TODO()
                """
            )

            simpleSingleSourceTarget(
                "d", """
                interface ABCD
                fun x(): ABCD = TODO()
                """
            )
        }

        result.assertCommonized(
            "a", """
            interface ABCD
            fun x(): ABCD = TODO()
            """
        )

        result.assertCommonized(
            "b", """
            interface ABCD
            fun x(): ABCD = TODO()
            """
        )

        result.assertCommonized(
            "c", """
            interface ABCD
            fun x(): ABCD = TODO()
            """
        )

        result.assertCommonized(
            "d", """
            interface ABCD
            fun x(): ABCD = TODO()
            """
        )

        result.assertCommonized(
            "(a, b)", """
            expect interface ABCD
            expect fun x(): ABCD
            """
        )

        result.assertCommonized(
            "(c, d)", """
            expect interface ABCD
            expect fun x(): ABCD
            """
        )

        result.assertCommonized(
            "((a,b), (c,d))", """
            expect interface ABCD
            expect fun x(): ABCD
            """
        )
    }

    fun `test function with returnType from dependency 1`() {
        val result = commonize {
            outputTarget("((a,b), (c,d))")
            registerDependency("((a,b), (c,d))") { source("interface ABCD") }
            simpleSingleSourceTarget("a", "fun x(): ABCD = TODO()")
            simpleSingleSourceTarget("b", "fun x(): ABCD = TODO()")
            simpleSingleSourceTarget("c", "fun x(): ABCD = TODO()")
            simpleSingleSourceTarget("d", "fun x(): ABCD = TODO()")
        }

        result.assertCommonized("a", "fun x(): ABCD")
        result.assertCommonized("b", "fun x(): ABCD")
        result.assertCommonized("c", "fun x(): ABCD")
        result.assertCommonized("d", "fun x(): ABCD")
        result.assertCommonized("(c, d)", "expect fun x(): ABCD")
        result.assertCommonized("(a, b)", "expect fun x(): ABCD")
        result.assertCommonized("((a,b), (c,d))", "expect fun x(): ABCD")
    }

    fun `test function with returnType from dependency 2`() {
        val result = commonize {
            outputTarget("((a,b), (c,d))")
            registerDependency("(a,b)") { source("interface ABCD") }
            registerDependency("(c,d)") { source("interface ABCD") }
            simpleSingleSourceTarget("a", "fun x(): ABCD = TODO()")
            simpleSingleSourceTarget("b", "fun x(): ABCD = TODO()")
            simpleSingleSourceTarget("c", "fun x(): ABCD = TODO()")
            simpleSingleSourceTarget("d", "fun x(): ABCD = TODO()")
        }

        result.assertCommonized("a", "fun x(): ABCD")
        result.assertCommonized("b", "fun x(): ABCD")
        result.assertCommonized("c", "fun x(): ABCD")
        result.assertCommonized("d", "fun x(): ABCD")
        result.assertCommonized("(c, d)", "expect fun x(): ABCD")
        result.assertCommonized("(a, b)", "expect fun x(): ABCD")
        result.assertCommonized("((a,b), (c,d))", "")
    }

    fun `test function with returnType from dependency 3`() {
        val result = commonize {
            outputTarget("((a,b), c)")
            registerDependency("((a,b), c)") { source("interface ABCD") }
            registerDependency("(a,b)") { source("interface ABCD") }
            simpleSingleSourceTarget("a", "fun x(): ABCD = TODO()")
            simpleSingleSourceTarget("b", "fun x(): ABCD = TODO()")
            simpleSingleSourceTarget("c", "fun x(): ABCD = TODO()")
        }

        result.assertCommonized("a", "fun x(): ABCD")
        result.assertCommonized("b", "fun x(): ABCD")
        result.assertCommonized("c", "fun x(): ABCD")
        result.assertCommonized("(a, b)", "expect fun x(): ABCD")
        result.assertCommonized("((a,b), c)", "expect fun x(): ABCD")
    }
}
