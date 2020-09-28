package com.jetbrains.kotlin.structuralsearch.replace

import com.jetbrains.kotlin.structuralsearch.KotlinSSRReplaceTest

class KotlinSSRPropertyReplaceTest : KotlinSSRReplaceTest() {
    fun testPropertyValueReplaceExplicitType() {
        doTest(
            searchPattern = "val '_ID : String = '_INIT",
            replacePattern = "val '_ID = \"foo\"",
            match = "val foo: String = \"bar\"",
            result = "val foo: String = \"foo\""
        )
    }

    fun testPropertyValueReplaceExplicitTypeFormatCopy() {
        doTest(
            searchPattern = "val '_ID : String = '_INIT",
            replacePattern = "val '_ID = \"foo\"",
            match = "val  foo :  String  =  \"bar\"",
            result = "val foo :  String  = \"foo\""
        )
    }
}