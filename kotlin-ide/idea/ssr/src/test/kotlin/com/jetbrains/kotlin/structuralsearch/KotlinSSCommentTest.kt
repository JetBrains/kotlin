package com.jetbrains.kotlin.structuralsearch

class KotlinSSCommentTest : KotlinSSTest() {
    override fun getBasePath() = "comment"

    /**
     * EOL
     */

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

    fun testBlockInProperty() { doTest("val '_ /**/ = '_") }

    fun testBlockBeforeClass() { doTest("""
        /**/
        class '_
    """.trimIndent()) }

    fun testBlockBeforeProperty() { doTest("""
        /**/
        val '_ = '_
    """.trimIndent()) }

    /**
     * KDoc
     */

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