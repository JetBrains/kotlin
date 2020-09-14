package com.jetbrains.kotlin.structuralsearch.replace

import com.jetbrains.kotlin.structuralsearch.KotlinSSRReplaceTest

class KotlinClassReplaceTest : KotlinSSRReplaceTest() {
    fun testConstructorParameterFormatCopy() {
        doTest(
            searchPattern = " class '_ID('_PARAM : '_TYPE)",
            replacePattern = "class '_ID('_PARAM : '_TYPE)",
            match = "class Foo(bar : Int  =  0)  {}",
            result = "class Foo(bar : Int  =  0)  {}"
        )
    }
}