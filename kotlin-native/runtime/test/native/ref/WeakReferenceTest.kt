/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.native.ref

import kotlin.native.ref.WeakReference
import kotlin.test.*

class WeakReferenceTest {
    @Test
    fun string() {
        val v = "Hello"
        val weak = WeakReference(v)
        assertEquals(v, weak.value)
    }

    @Test
    fun int() {
        val v = 0
        val weak = WeakReference(v)
        assertEquals(0, weak.value)
    }

    @Test
    fun long() {
        val v = Long.MAX_VALUE
        val weak = WeakReference(v)
        assertEquals(Long.MAX_VALUE, weak.value)
    }
}