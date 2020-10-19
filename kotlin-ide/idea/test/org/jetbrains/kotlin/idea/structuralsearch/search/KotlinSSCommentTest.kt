package org.jetbrains.kotlin.idea.structuralsearch.search

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSCommentTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath() = "comment"

    /**
     * EOL
     */

    fun testEol() { doTest("//") }

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

    fun testRegex() { doTest("// '_a:[regex( bar. )] = '_b:[regex( foo. )]") }

    /**
     * KDoc
     */

    fun testKDoc() { doTest("""
        /**
         *
         */
    """.trimIndent()) }

    fun testKDocTag() { doTest("""
        /**
         * @'_ '_
         */
    """.trimIndent()) }

    fun testKDocClass() { doTest("""
        /**
         *
         */
        class '_
    """.trimIndent()) }

    fun testKDocProperty() { doTest("""
        /**
         *
         */
        val '_ = '_
    """.trimIndent()) }

}