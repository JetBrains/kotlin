package com.jetbrains.kotlin.structuralsearch

class KotlinSSStringTemplateEntryTest : KotlinSSTest() {
    override fun getBasePath() = "stringTemplateEntry"

    fun testLiteral() { doTest(""" "foo" """) }

    fun testSimpleName() { doTest(""" "${"$"}foo" """) }

    fun testLongTemplate() { doTest(""" "${"$"}{1 + 1}" """) }

    fun testEscape() { doTest(""" "foo\\n" """) }

    fun testNested() { doTest(""" "${"$"}foo + 1 = ${"$"}{"${"$"}{foo + 1}"}" """) }

    fun testSingleVariable() { doTest(""" "'_" """) }

}