package org.jetbrains.kotlin.idea.structuralsearch.replace

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSRReplaceTest

class KotlinSSRQualifiedExpressionReplaceTest : KotlinSSRReplaceTest() {
    fun testQualifiedExpressionReceiverWithCountFilter() {
        doTest(
            searchPattern = "'_BEFORE{0,1}.'_FUN()",
            replacePattern = "'_BEFORE.foo('_ARG)",
            match = """
                fun main() {
                    bar()
                }
            """.trimIndent(),
            result = """
                fun main() {
                    foo()
                }
            """.trimIndent()
        )
    }
}