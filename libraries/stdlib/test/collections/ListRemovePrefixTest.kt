/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.Test
import kotlin.test.assertEquals

class ListRemovePrefixTest {

    val data = listOf(1, 2, 5, 8, 1)

    @Test
    fun removePrefixSingle() {
        assertEquals(listOf(2, 5, 8, 1), data.removePrefix(listOf(1)))
    }

    @Test
    fun removeEmptyPrefix() {
        assertEquals(data, data.removePrefix(emptyList()))
        assertEquals(data, data.removePrefix(listOf()))
    }

    @Test
    fun removePrefixBiggerThanList() {
        assertEquals(data, data.removePrefix(listOf(1, 2, 5, 8, 1, 0, 1)))
    }

    @Test
    fun removePrefixSameSize() {
        assertEquals(emptyList(), data.removePrefix(data))
    }

    @Test
    fun removePrefixWithNull() {
        assertEquals(listOf(null, 2), listOf(null, 1, null, 2).removePrefix(listOf(null, 1)))
    }

}