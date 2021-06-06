/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.Test
import kotlin.test.assertEquals

class StartWithListTest {

    private val data = listOf(1, 2, 8, 9)

    @Test
    fun startWithSingle() {
        assertEquals(true, data.startWith(listOf(1)))
    }

    @Test
    fun startWithPositive() {
        assertEquals(true, data.startWith(listOf(1)))
    }

    @Test
    fun startWithEmpty() {
        assertEquals(true, data.startWith(emptyList()))
    }

    @Test
    fun startWithNegative() {
        assertEquals(false, data.startWith(listOf(2, 8)))
    }
}