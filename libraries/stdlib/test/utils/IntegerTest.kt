/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class IntegerTest {

    @Test
    fun orZero() {
        val value: Int? = 43
        val valueNull: Int? = null

        assertEquals(43, value.orZero())
        assertEquals(0, valueNull.orZero())
    }

    @Test
    fun isNotNullOrZero() {
        val value: Int = 100
        val valueNull: Int? = null
        val valueZero: Int = 0

        assertEquals(100, value.isNotNullOrZero())
        assertEquals(0, valueNull.isNotNullOrZero())
        assertEquals(0, valueZero.isNotNullOrZero())
    }

    @Test
    fun isNotNullOrZeroLambda() {
        val value: Int = 100
        val valueNull: Int? = null
        val valueZero: Int = 0

        value.isNotNullOrZero {
            assertEquals(100, it)
        }
        valueNull.isNotNullOrZero {
            assertEquals(0, it)
        }
        valueZero.isNotNullOrZero {
            assertEquals(0, it)
        }
    }

}