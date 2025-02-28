/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.jetbrains.kotlin.gradle.testing.AssertionTypeCollapsedToAny
import org.jetbrains.kotlin.gradle.testing.PP
import org.jetbrains.kotlin.gradle.testing.assertEqualsPP
import kotlin.test.Test
import kotlin.test.assertEquals

class PPTest {
    @Test
    fun testAssertionType() {
        assertEquals(
            AssertionTypeCollapsedToAny,
            kotlin.runCatching { assertEqualsPP("1", 1) }.exceptionOrNull(),
        )
    }

    @Test
    fun testList() {
        assertEquals(
            """
                mutableListOf(
                  "1",
                  mutableListOf(
                    1,
                  ),
                )
            """.trimIndent(),
            PP(listOf("1", listOf(1)), 0).toString(),
        )
    }

    @Test
    fun testMap() {
        assertEquals(
            """
                mutableMapOf(
                  "baz" to mutableMapOf(
                    "a" to 1,
                  ),
                  "foo" to "bar",
                )
            """.trimIndent(),
            PP(mapOf("foo" to "bar", "baz" to mapOf("a" to 1)), 0).toString(),
        )
    }

    @Test
    fun testClass() {
        data class A(
            val a: Int,
            val b: String,
        )
        data class B(val m: Map<String, A>)

        assertEquals(
            """
                mutableMapOf(
                  A(
                  a = 1,
                  b = "1",
                ) to B(
                    m = mutableMapOf(
                      "baz" to A(
                        a = 2,
                        b = "2",
                      ),
                    ),
                  ),
                )
            """.trimIndent(),
            PP(
                mapOf(
                    A(1, "1") to B(mapOf("baz" to A(2, "2")))
                ),
                0
            ).toString(),
        )
    }
}