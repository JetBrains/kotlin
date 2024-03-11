/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.native.concurrent

import kotlin.native.concurrent.*
import kotlin.test.*

private class A(var x: Int)

@OptIn(FreezingIsDeprecated::class)
class FreezingTest {
    @Test
    fun freezeIsNoopForObjects() {
        val a = A(1)
        a.freeze()
        a.x = 2
        assertEquals(2, a.x)
    }

    @Test
    fun freezeIsNoopForArrays() {
        val a = arrayOf(1, 2, 3)
        a.freeze()
        a[0] = 4
        assertContentEquals(arrayOf(4, 2, 3), a)
    }

    @Test
    fun freezeIsNoopForPrimitiveArrays() {
        val a = intArrayOf(1, 2, 3)
        a.freeze()
        a[0] = 4
        assertContentEquals(intArrayOf(4, 2, 3), a)
    }
}