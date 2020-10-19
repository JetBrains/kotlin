package org.jetbrains.kotlin.idea.structuralsearch.replace

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSRReplaceTest

class KotlinSSRTargetedReplaceTest : KotlinSSRReplaceTest() {
    fun testTargetedField() {
        doTest(
            searchPattern = """
                class '_Class {  
                    val 'Field+ = '_Init?
                }
            """.trimIndent(),
            replacePattern = """
                val '_Field = 1
            """.trimIndent(),
            match = """
                class Foo {  
                    val bar = 0
                }
            """.trimIndent(),
            result = """
                class Foo {  
                    val bar = 1
                }
            """.trimIndent()
        )
    }

    fun testTargetedFunction() {
        doTest(
            searchPattern = """
                class '_Class {  
                    fun 'Fun()
                }
            """.trimIndent(),
            replacePattern = "fun '_Fun()",
            match = """
                class Foo {  
                    fun bar(): Int = 0
                }
            """.trimIndent(),
            result = """
                class Foo {  
                    fun bar(): Int = 0
                }
            """.trimIndent()
        )
    }

    fun testNestedFunction() {
        doTest(
            searchPattern = "'Fun('_Arg)",
            replacePattern = "'_Fun(\"foo\")",
            match = """
                fun foo(bar: String) {}

                fun main() {
                    foo("bar")
                }
            """.trimIndent(),
            result = """
                fun foo(bar: String) {}

                fun main() {
                    foo("foo")
                }
            """.trimIndent()
        )
    }
}