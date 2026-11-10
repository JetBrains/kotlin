/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.misc

import samples.*
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BooleanSamples {

    @Sample
    fun and() {
        // `and` checks if both operands are true
        assertFalse(false and false)
        assertFalse(false and true)
        assertFalse(true and false)
        assertTrue(true and true)
        // Unlike `&&`, `and` always evaluates both operands
        assertFailsWith<IllegalStateException> { false and error("Right operand evaluated") }
        assertFalse(false && error("Right operand evaluated"))
    }

    @Sample
    fun or() {
        // `or` checks if any of the operands is true
        assertFalse(false or false)
        assertTrue(false or true)
        assertTrue(true or false)
        assertTrue(true or true)
        // Unlike `||`, `or` always evaluates both operands
        assertFailsWith<IllegalStateException> { true or error("Right operand evaluated") }
        assertTrue(true || error("Right operand evaluated"))
    }

    @Sample
    fun xor() {
        // `xor` checks if operands are different
        assertFalse(false xor false)
        assertTrue(false xor true)
        assertTrue(true xor false)
        assertFalse(true xor true)
    }

    @Sample
    fun compareTo() {
        // false is equal to false, and true is equal to true
        assertPrints(false.compareTo(false), "0")
        assertPrints(true.compareTo(true), "0")

        // But false is less than true, and true is greater than false
        assertPrints(false.compareTo(true) < 0, "true")
        assertPrints(true.compareTo(false) > 0, "true")
    }
}
