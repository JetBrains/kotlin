package com.jetbrains.kotlin.structuralsearch

class KotlinSSCommentTest : KotlinSSTest() {
    override fun getBasePath() = "comment"

    fun testEolBeforeProperty() { doTest("""
        //
        val '_ = '_
    """.trimIndent()) }

    fun testEolInProperty() { doTest("val '_ = '_ //") }

    fun testBlockInProperty() { doTest("val '_ /**/ = '_") }

    fun testBlockBeforeClass() { doTest("""
        /**/
        class '_
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