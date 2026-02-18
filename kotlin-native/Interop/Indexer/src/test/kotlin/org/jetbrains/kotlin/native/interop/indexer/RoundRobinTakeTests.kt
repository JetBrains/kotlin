/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.indexer

import kotlin.test.Test
import kotlin.test.assertEquals

class RoundRobinTakeTests {

    @Test
    fun `limit of zero`() {
        val input = mapOf(
                "module1" to listOf<String>(),
        )
        assertEquals(
                roundRobinTake(input, 0),
                mapOf<String, List<String>>()
        )
    }
    @Test
    fun `module without errors`() {
        val input = mapOf(
                "module1" to listOf<String>(),
        )
        assertEquals(
                roundRobinTake(input, 10),
                mapOf<String, List<String>>(
                        "module1" to listOf()
                )
        )
    }

    @Test
    fun `empty input`() {
        val input = mapOf<String, List<String>>()
        assertEquals(
                mapOf<String, List<String>>(),
                roundRobinTake(input, 10),
        )
    }

    @Test
    fun `one module - with overflowing number of errors`() {
        val limit = 3
        val input = mapOf(
                "module1" to listOf("m1-0", "m1-1", "m1-2", "m1-3"),
        )
        assertEquals(
                mapOf<String, List<String>>(
                        "module1" to listOf("m1-0", "m1-1", "m1-2")
                ),
                roundRobinTake(input, limit),
        )
    }

    @Test
    fun `one module - with insufficient number of errors`() {
        val limit = 3
        val input = mapOf(
                "module1" to listOf("m1-0", "m1-1"),
        )
        assertEquals(
                mapOf<String, List<String>>(
                        "module1" to listOf("m1-0", "m1-1")
                ),
                roundRobinTake(input, limit),
        )
    }

    @Test
    fun `two modules`() {
        val input = mapOf(
                "module1" to listOf("m1-0", "m1-1"),
                "module2" to listOf("m2-0", "m2-1"),
        )
        assertEquals(
                mapOf<String, List<String>>(
                        "module1" to listOf("m1-0")
                ),
                roundRobinTake(input, 1),
        )
        assertEquals(
                mapOf<String, List<String>>(
                        "module1" to listOf("m1-0"),
                        "module2" to listOf("m2-0")
                ),
                roundRobinTake(input, 2),
        )
        assertEquals(
                mapOf<String, List<String>>(
                        "module1" to listOf("m1-0", "m1-1"),
                        "module2" to listOf("m2-0")
                ),
                roundRobinTake(input, 3),
        )
        assertEquals(
                input,
                roundRobinTake(input, 4),
        )
        assertEquals(
                input,
                roundRobinTake(input, 5),
        )
    }

    @Test
    fun `two modules - 1 missing errors`() {
        val input = mapOf(
                "module1" to listOf("m1-0", "m1-1"),
                "module2" to listOf(),
        )
        assertEquals(
                mapOf<String, List<String>>(
                        "module1" to listOf("m1-0")
                ),
                roundRobinTake(input, 1),
        )
        assertEquals(
                input,
                roundRobinTake(input, 2),
        )
        assertEquals(
                input,
                roundRobinTake(input, 3),
        )
    }


    @Test
    fun `three modules`() {
        val input = mapOf(
                "module1" to listOf("m1-0", "m1-1"),
                "module2" to listOf("m2-0"),
                "module3" to listOf("m3-0", "m3-1", "m3-2"),
        )
        assertEquals(
                mapOf(
                        "module1" to listOf("m1-0"),
                ),
                roundRobinTake(input, 1),
        )
        assertEquals(
                mapOf(
                        "module1" to listOf("m1-0"),
                        "module2" to listOf("m2-0"),
                ),
                roundRobinTake(input, 2),
        )
        assertEquals(
                mapOf(
                        "module1" to listOf("m1-0"),
                        "module2" to listOf("m2-0"),
                        "module3" to listOf("m3-0"),
                ),
                roundRobinTake(input, 3),
        )
        assertEquals(
                mapOf(
                        "module1" to listOf("m1-0", "m1-1"),
                        "module2" to listOf("m2-0"),
                        "module3" to listOf("m3-0"),
                ),
                roundRobinTake(input, 4),
        )
        assertEquals(
                mapOf(
                        "module1" to listOf("m1-0", "m1-1"),
                        "module2" to listOf("m2-0"),
                        "module3" to listOf("m3-0", "m3-1"),
                ),
                roundRobinTake(input, 5),
        )
        assertEquals(
                input,
                roundRobinTake(input, 6),
        )
        assertEquals(
                input,
                roundRobinTake(input, 7),
        )
    }
}