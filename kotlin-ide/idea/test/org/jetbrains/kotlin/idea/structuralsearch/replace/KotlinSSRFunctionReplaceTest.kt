package org.jetbrains.kotlin.idea.structuralsearch.replace

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSRReplaceTest

class KotlinSSRFunctionReplaceTest : KotlinSSRReplaceTest() {
    fun testVisibilityModifierCopy() {
        doTest(
            searchPattern = "fun '_ID('_PARAM*)",
            replacePattern = "fun '_ID('_PARAM)",
            match = "public fun foo() {}",
            result = "public fun foo() {}"
        )
    }

    fun testVisibilityModifierRemoval() {
        doTest(
            searchPattern = "public fun '_ID('_PARAM*)",
            replacePattern = "fun '_ID('_PARAM)",
            match = "public fun foo() {}",
            result = "fun foo() {}"
        )
    }

    fun testVisibilityModifierReplace() {
        doTest(
            searchPattern = "public fun '_ID('_PARAM*)",
            replacePattern = "private fun '_ID('_PARAM)",
            match = "public fun foo() {}",
            result = "private fun foo() {}"
        )
    }

    fun testVisibilityModifierFormatCopy() {
        doTest(
            searchPattern = "fun '_ID('_PARAM*)",
            replacePattern = "fun '_ID('_PARAM)",
            match = "public  fun foo() {}",
            result = "public  fun foo() {}"
        )
    }
    
    fun testFunctionParameterFormatCopy() {
        doTest(
            searchPattern = "fun '_ID('_PARAM)",
            replacePattern = "fun '_ID('_PARAM)",
            match = "public fun foo(bar  :  Int  =  0)  {}",
            result = "public fun foo(bar  :  Int  =  0)  {}"
        )
    }

    fun testFunctionTypedParameterFormatCopy() {
        doTest(
            searchPattern = "fun '_ID('_PARAM : '_TYPE)",
            replacePattern = "fun '_ID('_PARAM : '_TYPE)",
            match = "public fun foo(bar : Int  =  0)  {}",
            result = "public fun foo(bar : Int  =  0)  {}"
        )
    }

    fun testFunctionDefaultParameterFormatCopy() {
        doTest(
            searchPattern = "fun '_ID('_PARAM : '_TYPE = '_INIT)",
            replacePattern = "fun '_ID('_PARAM : '_TYPE = '_INIT)",
            match = "public fun foo(bar : Int = 0)  {}",
            result = "public fun foo(bar : Int = 0)  {}"
        )
    }

    fun testFunctionMultiParamCountFilter() {
        doTest(
            searchPattern = "fun '_ID('_PARAM*)",
            replacePattern = "fun '_ID('_PARAM)",
            match = "fun foo(one: Int, two: Double) {}",
            result = "fun foo(one: Int, two: Double) {}"
        )
    }

    fun testFunctionInitializer() {
        doTest(
            searchPattern = "'_ID()",
            replacePattern = "'_ID()",
            match = """
                    class Foo
                    fun foo() = Foo()
                """.trimIndent(),
            result = """
                    class Foo
                    fun foo() = Foo()
                """.trimIndent()
        )
    }

    fun testDotQualifiedSearchPattern() {
        doTest(
            searchPattern = "fun '_RECEIVER{0,1}.'_COLLECTOR()",
            replacePattern = "fun '_RECEIVER.'_COLLECTOR()",
            match = """
                    fun foo() { }
                """.trimIndent(),
            result = """
                    fun foo() { }
                """.trimIndent()
        )
    }
}