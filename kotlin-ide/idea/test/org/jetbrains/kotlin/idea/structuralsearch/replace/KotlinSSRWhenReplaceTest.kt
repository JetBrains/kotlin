package org.jetbrains.kotlin.idea.structuralsearch.replace

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSRReplaceTest

class KotlinSSRWhenReplaceTest : KotlinSSRReplaceTest() {
    fun testWhenKeyValueSwap() {
        doTest(
            searchPattern = """
                when ('_VAR) {
                    '_KEY -> '_VALUE
                }
            """.trimIndent(),
            replacePattern = """
                when ('_VAR) {
                    '_VALUE -> '_KEY
                }
            """.trimIndent(),
            match = """
                fun main() {
                    var x = 0
                    when (x) {
                        0 -> 1
                    }
                }
            """.trimIndent(),
            result = """
                fun main() {
                    var x = 0
                    when (x) {
                        1 -> 0
                    }
                }
            """.trimIndent()
        )
    }

    fun testWhenConditionCountFilter() {
        doTest(
            searchPattern = """
                when ('_VAR{0,1}) {
                    '_KEY -> '_VALUE
                }
            """.trimIndent(),
            replacePattern = """
                when('_VAR) {
                    '_KEY -> '_VALUE
                }
            """.trimIndent(),
            match = """
                fun main() {
                    var x = 0
                    when {
                        x == 0 -> 0
                    }
                }
            """.trimIndent(),
            result = """
                fun main() {
                    var x = 0
                    when {
                        x == 0 -> 0
                    }
                }
            """.trimIndent()
        )
    }
}