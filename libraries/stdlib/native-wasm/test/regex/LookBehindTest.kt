/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text.regex

import kotlin.test.Test
import kotlin.test.assertEquals

class LookBehindTest {
    private fun testFindAll(pattern: String, input: String, expectedTokens: List<String>) {
        val regex = Regex(pattern)
        val matches = regex.findAll(input)
        assertEquals(expectedTokens, matches.map { it.value }.toList())
    }

    @Test
    fun testPositiveLookBehind() {
        testFindAll("(?<=\\s)[a-z]*", "hi how are you doing", listOf("how", "are", "you", "doing"))
        testFindAll("(?<=\\.).", "bla, bla, bla", emptyList())
        testFindAll("(?<=a.c).", "ac0 abc1 abc", listOf("1"))
        testFindAll("(?<=a.c).", "abc0 abc1 abc", listOf("0", "1"))
        testFindAll("(?<=a.c).", "_abc0 abc1 abc", listOf("0", "1"))
        testFindAll("(?<=a.c).", "abc0 abbc1 a\nc2", listOf("0"))
    }

    @Test
    fun testNegativeLookBehind() {
        testFindAll("(?<!_)[_]+", "__ ___    ____", listOf("__", "___", "____"))
        testFindAll("(?<!a.c).", "bc0 abc1", listOf("b", "c", "0", " ", "a", "b", "c"))
        testFindAll("(?<!a.c).", "a\nc", listOf("a", "c"))
    }
}
