/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.Test
import kotlin.test.assertEquals

class StartsWithListTest {

    private val data = listOf(1, 2, 8, 9)

    @Test
    fun startsWithSingle() {
        assertEquals(true, data.startsWith(listOf(1)))
    }

    @Test
    fun startsWithPositive() {
        assertEquals(true, data.startsWith(listOf(1)))
    }

    @Test
    fun startsWithEmpty() {
        assertEquals(true, data.startsWith(emptyList()))
    }

    @Test
    fun startsWithNegative() {
        assertEquals(false, data.startsWith(listOf(2, 8)))
    }
}