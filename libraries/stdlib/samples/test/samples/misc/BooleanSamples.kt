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
        assertTrue(true and true)
        assertFalse(false and false)

        // And checks if both operands are true
        assertFalse(true and false)

        // And always evaluates both operands unlike &&
        assertFailsWith<IllegalStateException> { false and error("Right operand evaluated") }
        assertFalse(false && error("Right operand evaluated"))
    }

    @Sample
    fun or() {
        assertTrue(true or true)
        assertFalse(false or false)

        // And checks if any of the operands is true
        assertTrue(true or false)

        // And always evaluates both operands unlike &&
        assertFailsWith<IllegalStateException> { true or error("Right operand evaluated") }
        assertTrue(true || error("Right operand evaluated"))
    }

    @Sample
    fun xor() {
        // Xor checks if operands are different
        assertFalse(true xor true)
        assertFalse(false xor false)
        assertTrue(true xor false)
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
