/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.numbers

import kotlin.test.*


class BuiltinCompanionJVMTest {
    @Test fun intTest() {
        val i = Int
        assertEquals(java.lang.Integer.MAX_VALUE, i.MAX_VALUE)
        assertEquals(java.lang.Integer.MIN_VALUE, i.MIN_VALUE)
    }

    @Test fun doubleTest() {
        val d = Double
        assertEquals(java.lang.Double.POSITIVE_INFINITY, d.POSITIVE_INFINITY)
        assertEquals(java.lang.Double.NEGATIVE_INFINITY, d.NEGATIVE_INFINITY)
        assertEquals(java.lang.Double.NaN, d.NaN)

        assertEquals(java.lang.Double.MAX_VALUE, d.MAX_VALUE)
        assertEquals(java.lang.Double.MIN_VALUE, d.MIN_VALUE)
    }

    @Test fun floatTest() {
        val f = Float
        assertEquals(java.lang.Float.POSITIVE_INFINITY, f.POSITIVE_INFINITY)
        assertEquals(java.lang.Float.NEGATIVE_INFINITY, f.NEGATIVE_INFINITY)
        assertEquals(java.lang.Float.NaN, f.NaN)

        assertEquals(java.lang.Float.MAX_VALUE, f.MAX_VALUE)
        assertEquals(java.lang.Float.MIN_VALUE, f.MIN_VALUE)
    }

    @Test fun longTest() {
        val l = Long
        assertEquals(java.lang.Long.MAX_VALUE, l.MAX_VALUE)
        assertEquals(java.lang.Long.MIN_VALUE, l.MIN_VALUE)
    }

    @Test fun shortTest() {
        val s = Short
        assertEquals(java.lang.Short.MAX_VALUE, s.MAX_VALUE)
        assertEquals(java.lang.Short.MIN_VALUE, s.MIN_VALUE)
    }

    @Test fun byteTest() {
        val b = Byte
        assertEquals(java.lang.Byte.MAX_VALUE, b.MAX_VALUE)
        assertEquals(java.lang.Byte.MIN_VALUE, b.MIN_VALUE)
    }

    @Test fun charTest() {
        val ch = Char
        assertEquals(java.lang.Character.MIN_VALUE, ch.MIN_VALUE)
        assertEquals(java.lang.Character.MAX_VALUE, ch.MAX_VALUE)
        assertEquals(java.lang.Character.MIN_SURROGATE, ch.MIN_SURROGATE)
        assertEquals(java.lang.Character.MAX_SURROGATE, ch.MAX_SURROGATE)
        assertEquals(java.lang.Character.MIN_LOW_SURROGATE, ch.MIN_LOW_SURROGATE)
        assertEquals(java.lang.Character.MAX_LOW_SURROGATE, ch.MAX_LOW_SURROGATE)
        assertEquals(java.lang.Character.MIN_HIGH_SURROGATE, ch.MIN_HIGH_SURROGATE)
        assertEquals(java.lang.Character.MAX_HIGH_SURROGATE, ch.MAX_HIGH_SURROGATE)
    }

    @Test fun stringTest() {
        val s = String
        s.CASE_INSENSITIVE_ORDER
    }
}
