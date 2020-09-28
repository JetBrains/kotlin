package com.jetbrains.kotlin.structuralsearch.replace

import com.jetbrains.kotlin.structuralsearch.KotlinSSRReplaceTest

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
}