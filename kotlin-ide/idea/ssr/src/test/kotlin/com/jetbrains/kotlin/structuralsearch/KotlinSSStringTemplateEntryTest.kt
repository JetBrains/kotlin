package com.jetbrains.kotlin.structuralsearch

class KotlinSSStringTemplateEntryTest : KotlinSSTest() {
    override fun getBasePath() = "stringTemplateEntry"

    fun testLiteral() { doTest("\"foo\"") }

    fun testSimpleName() { doTest("\"\$foo\"") }

    fun testLongTemplate() { doTest("\"\${1 + 1}\"") }
}