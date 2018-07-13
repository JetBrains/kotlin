/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.numbers

import kotlin.test.*

class BuiltinCompanionTest {

    @Test
    fun intTest() {
        val i = Int
        i.MAX_VALUE

        assertSame(Int, i)
    }

    @Test
    fun doubleTest() {
        val d = Double
        d.NaN

        assertSame(Double, d)
    }

    @Test
    fun floatTest() {
        val f = Float
        f.NEGATIVE_INFINITY

        assertSame(Float, f)
    }

    @Test
    fun longTest() {
        val l = Long
        l.MAX_VALUE

        assertSame(Long, l)
    }

    @Test
    fun shortTest() {
        val s = Short
        s.MIN_VALUE

        assertSame(Short, s)
    }

    @Test
    fun byteTest() {
        val b = Byte
        b.MAX_VALUE

        assertSame(Byte, b)
    }

    @Test
    fun charTest() {
        val ch = Char
        ch.MIN_SURROGATE

        assertSame(Char, ch)
    }

    @Test
    fun stringTest() {
        val s = String

        assertSame(String, s)
    }

    @Test
    fun booleanTest() {
        val b = Boolean
        assertSame(Boolean, b)
    }
}