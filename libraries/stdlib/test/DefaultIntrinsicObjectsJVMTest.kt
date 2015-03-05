/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.numbers

import kotlin.test.*

import java.util.*
import kotlin.test.*
import org.junit.Test as test

class DefaultObjectsExtensionsTest {
    test fun intTest() {
        val i = Int

        assertEquals(java.lang.Integer.MAX_VALUE, Int.MAX_VALUE)
        assertEquals(java.lang.Integer.MIN_VALUE, Int.MIN_VALUE)
    }

    test fun doubleTest() {
        val d = Double

        assertEquals(java.lang.Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        assertEquals(java.lang.Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
        assertEquals(java.lang.Double.NaN, Double.NaN)

        assertEquals(java.lang.Double.MAX_VALUE, Double.MAX_VALUE)
        assertEquals(java.lang.Double.MIN_VALUE, Double.MIN_VALUE)
    }

    test fun floatTest() {
        val f = Float

        assertEquals(java.lang.Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        assertEquals(java.lang.Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
        assertEquals(java.lang.Float.NaN, Float.NaN)

        assertEquals(java.lang.Float.MAX_VALUE, Float.MAX_VALUE)
        assertEquals(java.lang.Float.MIN_VALUE, Float.MIN_VALUE)
    }

    test fun longTest() {
        val l = Long

        assertEquals(java.lang.Long.MAX_VALUE, Long.MAX_VALUE)
        assertEquals(java.lang.Long.MIN_VALUE, Long.MIN_VALUE)
    }

    test fun shortTest() {
        val s = Short

        assertEquals(java.lang.Short.MAX_VALUE, Short.MAX_VALUE)
        assertEquals(java.lang.Short.MIN_VALUE, Short.MIN_VALUE)
    }

    test fun byteTest() {
        val b = Byte

        assertEquals(java.lang.Byte.MAX_VALUE, Byte.MAX_VALUE)
        assertEquals(java.lang.Byte.MIN_VALUE, Byte.MIN_VALUE)
    }

    test fun stringTest() {
        val s = String

        assertEquals(s, String)
    }
}
