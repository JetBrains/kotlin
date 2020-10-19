package org.jetbrains.kotlin.idea.structuralsearch.search

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSStringTemplateEntryTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "stringTemplateEntry"

    fun testLiteral() { doTest(""" "foo" """) }

    fun testLiteralRegex() { doTest(""" "'_:[regex( . )] + '_:[regex( . )]" """) }

    fun testSimpleName() { doTest(""" "${"$"}foo" """) }

    fun testLongTemplate() { doTest(""" "${"$"}{1 + 1}" """) }

    fun testEscape() { doTest(""" "foo\\n" """) }

    fun testNested() { doTest(""" "${"$"}foo + 1 = ${"$"}{"${"$"}{foo + 1}"}" """) }

    fun testSingleVariable() { doTest(""" "'_" """) }

    fun testAllStrings() { doTest(""" "$$'_*" """) }

    fun testStringsContainingLongTemplate() { doTest(""" "$$'_*${'$'}{ '_ }$$'_*" """) }

    fun testStringWithBinaryExpression() { doTest(""" "${"$"}{3 * 2 + 1}" """) }

    fun testStringBracesTemplate() { doTest(""" "Hello world! ${"$"}a" """) }

    fun testStringBracesTemplateQuery() { doTest(""" "Hello world! ${"$"}{a}" """) }
}