package com.jetbrains.kotlin.structuralsearch.replace

import com.jetbrains.kotlin.structuralsearch.KotlinSSRReplaceTest

class KotlinSSAnnotationTest : KotlinSSRReplaceTest() {
    fun testAnnotatedClassReplacement() {
        doTest(
            searchPattern = "class '_ID",
            replacePattern = "class '_ID",
            match = """
                annotation class Foo
                
                @Foo
                class Bar
            """.trimIndent(),
            result = """
                annotation class Foo
                
                @Foo
                class Bar
            """.trimIndent()
        )
    }
}