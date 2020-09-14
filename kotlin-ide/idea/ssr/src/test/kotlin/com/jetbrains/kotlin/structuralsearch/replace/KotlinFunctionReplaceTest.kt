package com.jetbrains.kotlin.structuralsearch.replace

import com.jetbrains.kotlin.structuralsearch.KotlinSSRReplaceTest

class KotlinFunctionReplaceTest : KotlinSSRReplaceTest() {
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

    fun testVisibilityModifierReplacement() {
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

// wait for kotlin compiler changes
//    fun testSuperTypeFormatCopy() {
//        doTest(
//            searchPattern = "class '_ID",
//            replacePattern = "class '_ID",
//            match = """
//                interface Foo
//                interface Bar
//
//                class FooBar  :  Foo  ,  Bar
//            """.trimIndent(),
//            result = """
//                interface Foo
//                interface Bar
//
//                class FooBar  :  Foo  ,  Bar
//            """.trimIndent()
//        )
//    }
}