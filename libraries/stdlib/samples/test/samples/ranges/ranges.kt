/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package samples.ranges

import samples.*
import java.sql.Date
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Ranges {

    @Sample
    fun rangeFromComparable() {
        val start = Date.valueOf("2017-01-01")
        val end = Date.valueOf("2017-12-31")
        val range = start..end
        assertPrints(range, "2017-01-01..2017-12-31")

        assertTrue(Date.valueOf("2017-05-27") in range)
        assertFalse(Date.valueOf("2018-01-01") in range)
        assertTrue(Date.valueOf("2018-01-01") !in range)
    }

    @Sample
    fun rangeFromDouble() {
        val range = 1.0..100.0
        assertPrints(range, "1.0..100.0")

        assertTrue(3.14 in range)
        assertFalse(100.1 in range)
    }

    @Sample
    fun rangeFromFloat() {
        val range = 1f..100f
        assertPrints(range, "1.0..100.0")

        assertTrue(3.14f in range)
        assertFalse(100.1f in range)
    }

    @Sample
    fun intStep() {
        val range = 1..6 step 2
        assertPrints(range, "[1, 3, 5]")

        assertTrue(1 in range)
        assertFalse(2 in range)
        assertTrue(3 in range)
    }

    @Sample
    fun longStep() {
        val range = 1L..6L step 2L
        assertPrints(range, "[1, 3, 5]")

        assertTrue(1L in range)
        assertFalse(2L in range)
        assertTrue(3L in range)
    }

    @Sample
    fun charStep() {
        val range = 'a'..'e' step 2
        assertPrints(range, "[a, c, e]")

        assertTrue('a' in range)
        assertFalse('b' in range)
        assertTrue('c' in range)
    }
}