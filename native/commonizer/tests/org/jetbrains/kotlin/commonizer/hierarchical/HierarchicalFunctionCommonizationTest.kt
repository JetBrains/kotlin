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
            outputTarget("(a,b)", "(c,d)", "(a, b, c, d)")
            simpleSingleSourceTarget("a", "fun x(): Int = 42")
            simpleSingleSourceTarget("b", "fun x(): Int = 42")
            simpleSingleSourceTarget("c", "fun x(): Int = 42")
            simpleSingleSourceTarget("d", "fun x(): Int = 42")
        }

        result.assertCommonized("((a,b), (c,d))", "expect fun x(): Int")
        result.assertCommonized("(a,b)", "expect fun x(): Int")
        result.assertCommonized("(c,d)", "expect fun x(): Int")
    }

    fun `test simple function 2`() {
        val result = commonize {
            outputTarget("(a, b)", "(a, b, c)")
            simpleSingleSourceTarget("a", "fun x(): Int = 42")
            simpleSingleSourceTarget("b", "fun x(): Int = 42")
            simpleSingleSourceTarget("c", "fun x(): Int = 42")
        }

        result.assertCommonized("((a,b), c)", "expect fun x(): Int")
        result.assertCommonized("(a,b)", "expect fun x(): Int")
    }

    fun `test function with returnType`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")
            simpleSingleSourceTarget(
                "a", """
                interface ABCD
                fun x(): ABCD = null!!
            """
            )
            simpleSingleSourceTarget(
                "b", """
                interface ABCD
                fun x(): ABCD = null!!
                """
            )
            simpleSingleSourceTarget(
                "c", """
                interface ABCD
                fun x(): ABCD = null!!
                """
            )

            simpleSingleSourceTarget(
                "d", """
                interface ABCD
                fun x(): ABCD = null!!
                """
            )
        }

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
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")
            registerDependency("a", "b", "c", "d", "(a, b)", "(c, d)", "(a, b, c, d)") { source("interface ABCD") }
            simpleSingleSourceTarget("a", "fun x(): ABCD = null!!")
            simpleSingleSourceTarget("b", "fun x(): ABCD = null!!")
            simpleSingleSourceTarget("c", "fun x(): ABCD = null!!")
            simpleSingleSourceTarget("d", "fun x(): ABCD = null!!")
        }

        result.assertCommonized("(c, d)", "expect fun x(): ABCD")
        result.assertCommonized("(a, b)", "expect fun x(): ABCD")
        result.assertCommonized("((a,b), (c,d))", "expect fun x(): ABCD")
    }

    fun `test function with returnType from dependency 2`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")
            registerDependency("a", "b", "c", "d") { source("interface ABCD") }
            registerDependency("(a, b)") { source("interface ABCD") }
            registerDependency("(c,d)") { source("interface ABCD") }
            simpleSingleSourceTarget("a", "fun x(): ABCD = null!!")
            simpleSingleSourceTarget("b", "fun x(): ABCD = null!!")
            simpleSingleSourceTarget("c", "fun x(): ABCD = null!!")
            simpleSingleSourceTarget("d", "fun x(): ABCD = null!!")
        }

        result.assertCommonized("(c, d)", "expect fun x(): ABCD")
        result.assertCommonized("(a, b)", "expect fun x(): ABCD")

        // ABCD is not given as dependency on (a, b, c, d) -> can't be commonized
        result.assertCommonized("((a,b), (c,d))", "")
    }

    fun `test function with returnType from dependency 3`() {
        val result = commonize {
            outputTarget("(a, b)", "(a, b, c)")
            registerDependency("(a, b, c)") { source("interface ABCD") }
            registerDependency("a", "b", "c", "(a, b)") { source("interface ABCD") }
            simpleSingleSourceTarget("a", "fun x(): ABCD = null!!")
            simpleSingleSourceTarget("b", "fun x(): ABCD = null!!")
            simpleSingleSourceTarget("c", "fun x(): ABCD = null!!")
        }

        result.assertCommonized("(a, b)", "expect fun x(): ABCD")
        result.assertCommonized("((a,b), c)", "expect fun x(): ABCD")
    }

    fun `test function with simple annotation`() {
        val result = commonize {
            outputTarget("(a, b)")
            registerDependency("a", "b", "(a, b)") { source("annotation class FooAnnotation") }
            simpleSingleSourceTarget("a", "@FooAnnotation fun x() = Unit")
            simpleSingleSourceTarget("b", "@FooAnnotation fun x() = Unit")
        }

        result.assertCommonized("(a, b)", "@FooAnnotation expect fun x()")
    }

    fun `test function with non-simple annotation - 1`() {
        val result = commonize {
            outputTarget("(a, b)")
            registerDependency("a", "b", "(a, b)") { source("annotation class FooAnnotation(val param: String)") }
            simpleSingleSourceTarget("a", """@FooAnnotation("a") fun x() = Unit""")
            simpleSingleSourceTarget("b", """@FooAnnotation("b") fun x() = Unit""")
        }

        result.assertCommonized("(a, b)", "expect fun x()")
    }

    fun `test function with non-simple annotation - 2`() {
        val result = commonize {
            outputTarget("(a, b)")
            registerDependency("a", "b", "(a, b)") { source("annotation class FooAnnotation<T: Any>(val param: String)") }
            simpleSingleSourceTarget("a", """@FooAnnotation<Unit>("a") fun x() = Unit""")
            simpleSingleSourceTarget("b", """@FooAnnotation<Unit>("b") fun x() = Unit""")
        }

        result.assertCommonized("(a, b)", "expect fun x()")
    }
}
