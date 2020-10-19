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
}