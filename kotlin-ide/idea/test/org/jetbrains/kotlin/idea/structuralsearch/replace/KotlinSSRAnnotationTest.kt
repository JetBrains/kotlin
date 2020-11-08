package org.jetbrains.kotlin.idea.structuralsearch.replace

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSRReplaceTest

class KotlinSSRAnnotationTest : KotlinSSRReplaceTest() {
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