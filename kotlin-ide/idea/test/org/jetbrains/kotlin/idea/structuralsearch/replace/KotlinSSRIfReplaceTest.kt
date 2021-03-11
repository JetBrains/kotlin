package org.jetbrains.kotlin.idea.structuralsearch.replace

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSRReplaceTest

class KotlinSSRIfReplaceTest : KotlinSSRReplaceTest() {
    fun testIfThenMultiLineFormat() {
        doTest(
            searchPattern = "if('_VAR) { '_BLOCK }",
            replacePattern = """
                if ('_VAR) {
                    '_BLOCK
                }
            """.trimIndent(),
            match = """
                fun main() {
                    var x = 0
                    if (x == 0) {
                        println()
                    }
                }
            """.trimIndent(),
            result = """
                fun main() {
                    var x = 0
                    if (x == 0) {
                        println()
                    }
                }
            """.trimIndent()
        )
    }

    fun testIfThenSingleLineFormat() {
        doTest(
            searchPattern = "if('_VAR) { '_BLOCK }",
            replacePattern = """
                if ('_VAR) { '_BLOCK }
            """.trimIndent(),
            match = """
                fun main() {
                    var x = 0
                    if (x == 0) {
                        println()
                    }
                }
            """.trimIndent(),
            result = """
                fun main() {
                    var x = 0
                    if (x == 0) { println() }
                }
            """.trimIndent()
        )
    }

    fun testIfThenNoBracketsFormat() {
        doTest(
            searchPattern = "if('_VAR) { '_BLOCK }",
            replacePattern = """
                if ('_VAR) '_BLOCK
            """.trimIndent(),
            match = """
                fun main() {
                    var x = 0
                    if (x == 0) {
                        println()
                    }
                }
            """.trimIndent(),
            result = """
                fun main() {
                    var x = 0
                    if (x == 0) println()
                }
            """.trimIndent()
        )
    }


    fun testIfElseNoBracketsFormat() {
        doTest(
            searchPattern = "if('_VAR) { '_BLOCK }",
            replacePattern = """
                if ('_VAR) '_BLOCK else '_BLOCK
            """.trimIndent(),
            match = """
                fun main() {
                    var x = 0
                    if (x == 0) {
                        println()
                    }
                }
            """.trimIndent(),
            result = """
                fun main() {
                    var x = 0
                    if (x == 0) println() else println()
                }
            """.trimIndent()
        )
    }
}