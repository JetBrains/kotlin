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

    fun testDoubleQualifiedExpression() {
        doTest(
            searchPattern = """
                '_REC.foo = '_INIT
                '_REC.bar = '_INIT
            """.trimIndent(),
            replacePattern = """
                '_REC.fooBar = '_INIT
            """.trimIndent(),
            match = """
                fun main() {
                    x.foo = true
                    x.bar = true
                }
            """.trimIndent(),
            result = """
                fun main() {
                    x.fooBar = true
                }
            """.trimIndent()
        )
    }
}