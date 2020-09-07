package com.jetbrains.kotlin.structuralsearch.replace

import com.jetbrains.kotlin.structuralsearch.KotlinSSRReplaceTest

class KotlinTargetedReplaceTest : KotlinSSRReplaceTest() {
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
            replacePattern = """
                fun '_Fun()
            """.trimIndent(),
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
}