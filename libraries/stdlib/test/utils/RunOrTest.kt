/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.test.*

class RunOrTest {
    @Test
    fun runOrNull_true_runsBlock() {
        val x = runOrNull(true) { "ran" }
        assertEquals("ran", x)
    }

    @Test
    fun runOrNull_false_blockNotRun() {
        val x = runOrNull(false) { throw AssertionError("shouldn't run") }
        assertNull(x)
    }

    @Test
    fun runOrNull_conditionAssumedInBlock() {
        fun nullableHelper(x: String?) = runOrNull(x != null) { x.reversed() } // requires contract to compile x.length
        assertEquals("olleh", nullableHelper("hello"))
        assertNull(nullableHelper(null))
    }

    @Test
    fun runOrDefault_true_runsBlock() {
        val x = runOrDefault(true, default = "didn't run") { "ran" }
        assertEquals("ran", x)
    }

    @Test
    fun runOrDefault_false_returnsDefault() {
        val x = runOrDefault(false, default = 42) { throw AssertionError("shouldn't run") }
        assertEquals(42, x)
    }

    @Test
    fun runOrDefault_conditionAssumedInBlock() {
        fun nullableHelper(x: String?) = runOrDefault(x != null, -1) { x.length } // requires contract to compile
        assertEquals(5, nullableHelper("hello"))
        assertEquals(-1, nullableHelper(null))
    }
}
