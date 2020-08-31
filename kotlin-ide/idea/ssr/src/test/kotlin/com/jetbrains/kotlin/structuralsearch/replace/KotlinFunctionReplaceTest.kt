package com.jetbrains.kotlin.structuralsearch.replace

import com.jetbrains.kotlin.structuralsearch.KotlinSSRReplaceTest

class KotlinFunctionReplaceTest : KotlinSSRReplaceTest() {
    fun testVisibilityModifierFormatCopy() {
        doTest(
            searchPattern = "fun '_ID('_PARAM*)",
            replacePattern = "fun '_ID('_PARAM)",
            match = "public  fun foo() {}",
            result = "public  fun foo() {}"
        )
    }
}