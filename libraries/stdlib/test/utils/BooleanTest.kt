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
    fun onTrueInvokeAtMostOnceContract() {
        // will fail to compile without AT_MOST_ONCE
        val value: Int
        true.onTrue {
            value = 1
            assertEquals(1, value)
        }
    }

    @Test
    fun onTrueHoldsInContract() {
        // will fail to compile without holdsIn
        val x: Any = "OK"
        (x is String).onTrue {
            assertEquals(2, x.length)
        }
    }

    @Test
    fun onTrueReturnsTrueContract() {
        // will fail to compile without `returns(true)-implies(this)`
        val x: Any = "OK"
        val result = (x is String).onTrue { }
        if (result) {
            assertEquals(2, x.length)
        }
    }

    @Test
    fun onTrueReturnsFalseContract() {
        // will fail to compile without `returns(false)-implies(!this)`
        val x: Any = "OK"
        val result = (x !is String).onTrue { }
        if (!result) {
            assertEquals(2, x.length)
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
    fun onFalseInvokeAtMostOnceContract() {
        // will fail to compile without AT_MOST_ONCE
        val value: Int
        false.onFalse {
            value = 1
            assertEquals(1, value)
        }
    }

    @Test
    fun onFalseHoldsInContract() {
        // will fail to compile without holdsIn
        val x: Any = "OK"
        (x !is String).onFalse {
            assertEquals(2, x.length)
        }
    }

    @Test
    fun onFalseReturnsTrueContract() {
        // will fail to compile without `returns(true)-implies(this)`
        val x: Any = "OK"
        val result = (x is String).onFalse { }
        if (result) {
            assertEquals(2, x.length)
        }
    }

    @Test
    fun onFalseReturnsFalseContract() {
        // will fail to compile without `returns(false)-implies(!this)`
        val x: Any = "OK"
        val result = (x !is String).onFalse { }
        if (!result) {
            assertEquals(2, x.length)
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
    fun ifOrNullInvokeAtMostOnceContract() {
        // will fail to compile without AT_MOST_ONCE
        val value: Int
        val result = ifOrNull(true) {
            value = 1
            assertEquals(1, value)
            "OK"
        }
        assertEquals("OK", result)
    }

    @Test
    fun ifOrNullHoldsInContract() {
        // will fail to compile without holdsIn
        val x: Any = "OK"
        val result = ifOrNull(x is String) {
            x.length
        }
        assertEquals(2, result)
    }

    @Test
    fun ifOrNullReturnsNotNullContract() {
        // will fail to compile without `returnsNotNull-implies(condition)`
        val x: Any = "OK"
        val result = ifOrNull(x is String) {
            x.length
        }
        if (result != null) {
            assertEquals(x.length, result)
        }
    }
}

