package com.jetbrains.kotlin.structuralsearch.replace

import com.jetbrains.kotlin.structuralsearch.KotlinSSRReplaceTest

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