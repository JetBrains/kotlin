/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.test.*

private class Box(val value: Int)

class ScopeFunctionsTest {
    @Test
    fun testLet() {
        val box = Box(42)
        assertEquals(42, box.let { it.value })
    }

    @Test
    fun testWith() {
        val box = Box(42)
        assertEquals(42, with(box) { value })
    }

    @Test
    fun testRun() {
        assertEquals(42, run { 42 })
        val box = Box(42)
        assertEquals(42, box.run { value })
    }

    @Test
    fun testApply() {
        val box = Box(42)
        assertEquals(box, box.apply { value })
    }

    @Test
    fun testAlso() {
        val box = Box(42)
        assertEquals(box, box.also { it.value })
    }
}