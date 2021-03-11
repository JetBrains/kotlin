package org.jetbrains.kotlin.idea.structuralsearch.replace

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSRReplaceTest

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

    fun testPropertyNoInitializer() {
        doTest(
            searchPattern = "var '_ID : '_TYPE = '_INIT{0,1}",
            replacePattern = "var '_ID : '_TYPE = '_INIT",
            match = "var foo: String",
            result = "var foo : String"
        )
    }

    fun testPropertyInitializer() {
        doTest(
                searchPattern = "'_ID()",
                replacePattern = "'_ID()",
                match = """
                    class Foo
                    val foo = Foo()
                """.trimIndent(),
                result = """
                    class Foo
                    val foo = Foo()
                """.trimIndent()
        )
    }
}