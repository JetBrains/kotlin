/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BooleanTest {
    @Test
    fun onTrueIfTrue() {
        var value = 0
        val result = true.onTrue {
            ++value
        }
        assertEquals(1, value)
        assertTrue(result)
    }

    @Test
    fun onTrueIfFalse() {
        var value = 0
        val result = false.onTrue {
            ++value
        }
        assertEquals(0, value)
        assertFalse(result)
    }

    @Test
    fun onTrueContract() {
        // will fail to compile without AT_MOST_ONCE
        val value: Int
        true.onTrue {
            value = 1
            assertEquals(1, value)
        }
    }

    @Test
    fun onFalseIfTrue() {
        var value = 0
        val result = true.onFalse {
            ++value
        }
        assertEquals(0, value)
        assertTrue(result)
    }

    @Test
    fun onFalseIfFalse() {
        var value = 0
        val result = false.onFalse {
            ++value
        }
        assertEquals(1, value)
        assertFalse(result)
    }

    @Test
    fun onFalseContract() {
        // will fail to compile without AT_MOST_ONCE
        val value: Int
        false.onFalse {
            value = 1
            assertEquals(1, value)
        }
    }

    @Test
    fun ifOrNullIfTrue() {
        var value = 0
        val result = ifOrNull(true) {
            ++value
            "OK"
        }
        assertEquals(1, value)
        assertEquals("OK", result)
    }

    @Test
    fun ifOrNullIfFalse() {
        var value = 0
        val result = ifOrNull(false) {
            ++value
            "OK"
        }
        assertEquals(0, value)
        assertNull(result)
    }

    @Test
    fun ifOrNullContract() {
        // will fail to compile without AT_MOST_ONCE
        val value: Int
        val result = ifOrNull(true) {
            value = 1
            assertEquals(1, value)
            "OK"
        }
        assertEquals("OK", result)
    }
}

