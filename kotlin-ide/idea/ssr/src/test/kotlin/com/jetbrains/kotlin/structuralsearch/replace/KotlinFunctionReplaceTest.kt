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
}