/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package sample

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

fun requireInTest() {
    val greeting = "Hello, Test!"
    require(greeting == "Hello, World!")
}

fun assertInTest() {
    val greeting = "Hello, Test!"
    assert(greeting == "Hello, World!")
}

class Test {
    @Test
    fun testRequireMainSourceSet() {
        val error = assertFailsWith<IllegalArgumentException> {
            requireInMain()
        }
        assertEquals(
            actual = error.message,
            expected = """
                Assertion failed
                require(greeting == "Hello, World!")
                        |        |
                        |        false
                        Hello, Main!
            """.trimIndent(),
        )
    }

    @Test
    fun testAssertMainSourceSet() {
        try {
            assertInMain()
        } catch (e: AssertionError) {
            assertEquals("Assertion failed", e.message)
        }
    }

    @Test
    fun testRequireTestSourceSet() {
        try {
            requireInTest()
        } catch (e: IllegalArgumentException) {
            assertEquals("Failed requirement.", e.message)
        }
    }

    @Test
    fun testAssertTestSourceSet() {
        try {
            assertInTest()
        } catch (e: AssertionError) {
            assertEquals("Assertion failed", e.message)
        }
    }
}