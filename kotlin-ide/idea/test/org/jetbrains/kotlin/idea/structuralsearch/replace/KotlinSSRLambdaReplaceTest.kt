package org.jetbrains.kotlin.idea.structuralsearch.replace

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSRReplaceTest

class KotlinSSRLambdaReplaceTest : KotlinSSRReplaceTest() {
    fun testLambdaCountFilterParam() {
        doTest(
            searchPattern = "{ '_PARAM* -> '_EXPR* }",
            replacePattern = "{ '_PARAM -> '_EXPR }",
            match = """
                fun foo(bar: () -> Unit)
                
                fun main() {
                    foo { }
                }
            """.trimIndent(),
            """
                fun foo(bar: () -> Unit)
                
                fun main() {
                    foo {  }
                }
            """.trimIndent()
        )
    }

    fun testLambdaFullTemplate() {
        doTest(
            searchPattern = "{ '_LAMBDA }",
            replacePattern = "{ '_LAMBDA }",
            match = """
                fun foo(bar: (Int) -> Unit)
                
                fun main() {
                    foo { i -> println(i) }
                }
            """.trimIndent(),
            """
                fun foo(bar: (Int) -> Unit)
                
                fun main() {
                    foo { i -> println(i) }
                }
            """.trimIndent()
        )
    }

    fun testLambdaFullTemplateMultipleParameters() {
        doTest(
            searchPattern = "{ '_LAMBDA }",
            replacePattern = "{ '_LAMBDA }",
            match = """
                fun foo(bar: (Int, String, Int) -> Unit)
                
                fun main() {
                    foo { i,  s  ,  i -> println(i) }
                }
            """.trimIndent(),
            """
                fun foo(bar: (Int, String, Int) -> Unit)
                
                fun main() {
                    foo { i,  s  ,  i -> println(i) }
                }
            """.trimIndent()
        )
    }

    fun testLambdaFullTypedTemplate() {
        doTest(
            searchPattern = "{ '_LAMBDA }",
            replacePattern = "{ '_LAMBDA }",
            match = """
                fun foo(bar: (Int) -> Unit)
                
                fun main() {
                    foo { i: Int -> println(i) }
                }
            """.trimIndent(),
            """
                fun foo(bar: (Int) -> Unit)
                
                fun main() {
                    foo { i: Int -> println(i) }
                }
            """.trimIndent()
        )
    }

    fun testLambdaCallArgument() {
        doTest(
            searchPattern = "'_FUNCTION.map { '_LAMBDA }",
            replacePattern = "'_FUNCTION.map { '_LAMBDA }",
            match = """
                fun main() {
                    listOf().map { i -> 4*i }
                }
            """.trimIndent(),
            """
                fun main() {
                    listOf().map { i -> 4*i }
                }
            """.trimIndent()
        )
    }

    fun testLambdaTypedArgument() {
        doTest(
            searchPattern = "{ '_PARAM -> '_BODY }",
            replacePattern = "{ '_PARAM -> '_BODY }",
            match = """
                fun main() {
                    val f = { one: Int -> one.toShort() }
                }
            """.trimIndent(),
            """
                fun main() {
                    val f = { one: Int -> one.toShort() }
                }
            """.trimIndent()
        )
    }
}