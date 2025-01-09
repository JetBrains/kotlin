/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.abi.tools.filtering.wildcardsToRegex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PatternTests {
    @Test
    fun testMappingToRegex() {
        assertEquals("simple text", "simple text".wildcardsToRegex().pattern)
        // test escaping meta symbols <([{\^-=$!|]})+.>
        assertEquals(
            "\\(meta\\)\\^\\[symbols\\]:\\! \\| \\\\d\\=2\\+3\\.5 \\< 6 && 1€ \\> 1\\\$",
            "(meta)^[symbols]:! | \\d=2+3.5 < 6 && 1€ > 1$".wildcardsToRegex().pattern
        )

        assertEquals("[^.]*prefix", "*prefix".wildcardsToRegex().pattern)
        assertEquals(".*any prefix", "**any prefix".wildcardsToRegex().pattern)
        assertEquals("suffix[^.]*", "suffix*".wildcardsToRegex().pattern)
        assertEquals("any suffix.*", "any suffix**".wildcardsToRegex().pattern)
        assertEquals("qu.stion.", "qu?stion?".wildcardsToRegex().pattern)
        assertEquals("[^.]*mix.of.*wildcards[^.]*", "*mix?of**wildcards*".wildcardsToRegex().pattern)
    }

    @Test
    fun testNotNamesPatterns() {
        assertTrue("<([{\\^-=\$!|]})+.>".wildcardsToRegex().matches("<([{\\^-=\$!|]})+.>"))

        assertTrue("**com.example foo = bar**".wildcardsToRegex().matches("public class com.example foo = bar { public fun <init> ()V }"))
    }

    @Test
    fun testHierarchicalNames() {
        assertFilter("com.example.MyClass")
            .matches("com.example.MyClass")
            .miss("com.example.MyClass2", "com.example.", "example.MyClass")

        assertFilter("com.example.M?Class")
            .matches("com.example.MyClass", "com.example.MeClass", "com.example.M.Class")
            .miss("com.example.subpackage.MyClass", "com.example.subpackage.MClass")

        assertFilter("com.example.My*")
            .matches("com.example.MyClass", "com.example.My")
            .miss("com.example.subpackage.MyClass", "com.example.subpackage.My.Class")

        assertFilter("*.*.My*")
            .matches("com.example.MyClass", "a.b.My", "..My")
            .miss("com.example.subpackage.MyClass", "com.example.My.Class")

        assertFilter("com.**.MyClass")
            .matches("com.example.MyClass", "com.example.subpackage.MyClass", "com..MyClass")
            .miss("com.MyClass")

        assertFilter("com.**MyClass")
            .matches("com.MyClass,com.ExtraMyClass", "com.example.MyClass", "com.example.subpackage.ExtraMyClass")
            .miss("comMyClass", "MyClass")

        assertFilter("com.example.*?*")
            .matches("com.example.MyClass", "com.example.subclass.MyClass", "com.example.A")
            .miss("com.exampleClass", "com.example.a.b.c", "com.example.")
    }


    private class Asserter(private val filter: String) {
        private val regex: Regex = filter.wildcardsToRegex()

        fun matches(vararg strings: String): Asserter {
            strings.forEach { string ->
                assertTrue(regex.matches(string), "Text '$string' should matches to filter '$filter' (regex $regex)")
            }
            return this
        }

        fun miss(vararg strings: String): Asserter {
            strings.forEach { string ->
                assertFalse(regex.matches(string), "Text '$string' should not matches to filter '$filter'")
            }
            return this
        }
    }

    private fun assertFilter(filter: String): Asserter {
        return Asserter(filter)
    }

}