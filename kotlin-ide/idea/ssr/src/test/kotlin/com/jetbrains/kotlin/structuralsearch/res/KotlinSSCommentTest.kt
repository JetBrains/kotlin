package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSCommentTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath() = "comment"

    /**
     * EOL
     */

    fun testEol() { doTest("//") }

    fun testEolRegex() { doTest("// '_a:[regex( bar. )] = '_b:[regex( foo. )]") }

    fun testEolBeforeProperty() { doTest("""
        //
        val '_ = '_
    """.trimIndent()) }

    fun testEolBeforeClass() { doTest("""
        //
        class '_
    """.trimIndent()) }

    fun testEolInProperty() { doTest("val '_ = '_ //") }

    /**
     * Block
     */

    fun testBlock() { doTest("/**/") }

    /**
     * KDoc
     */

    fun testKdoc() { doTest("""
        /**
         *
         */
    """.trimIndent()) }


    fun testKdocTag() { doTest("""
        /**
         * @'_ '_
         */
    """.trimIndent()) }

    fun testKdocClass() { doTest("""
        /**
         *
         */
        class '_
    """.trimIndent()) }

    fun testKdocProperty() { doTest("""
        /**
         *
         */
        val '_ = '_
    """.trimIndent()) }

}