/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package samples.comparisons

import samples.*
import java.time.DayOfWeek
import kotlin.test.assertFailsWith

class ComparableOps {

    @Sample
    fun coerceAtLeast() {
        assertPrints(10.coerceAtLeast(5), "10")
        assertPrints(10.coerceAtLeast(20), "20")
    }

    @Sample
    fun coerceAtLeastUnsigned() {
        assertPrints(10u.coerceAtLeast(5u), "10")
        assertPrints(10u.coerceAtLeast(20u), "20")
    }

    @Sample
    fun coerceAtLeastComparable() {
        assertPrints(DayOfWeek.WEDNESDAY.coerceAtLeast(DayOfWeek.MONDAY), "WEDNESDAY")
        assertPrints(DayOfWeek.WEDNESDAY.coerceAtLeast(DayOfWeek.FRIDAY), "FRIDAY")
    }

    @Sample
    fun coerceAtMost() {
        assertPrints(10.coerceAtMost(5), "5")
        assertPrints(10.coerceAtMost(20), "10")
    }

    @Sample
    fun coerceAtMostUnsigned() {
        assertPrints(10u.coerceAtMost(5u), "5")
        assertPrints(10u.coerceAtMost(20u), "10")
    }

    @Sample
    fun coerceAtMostComparable() {
        assertPrints(DayOfWeek.FRIDAY.coerceAtMost(DayOfWeek.SATURDAY), "FRIDAY")
        assertPrints(DayOfWeek.FRIDAY.coerceAtMost(DayOfWeek.WEDNESDAY), "WEDNESDAY")
    }

    @Sample
    fun coerceIn() {
        assertPrints(10.coerceIn(1, 100), "10")
        assertPrints(10.coerceIn(1..100), "10")
        assertPrints(0.coerceIn(1, 100), "1")
        assertPrints(500.coerceIn(1, 100), "100")
        assertFailsWith<IllegalArgumentException> {
            10.coerceIn(100, 0)
        }
    }

    @Sample
    fun coerceInUnsigned() {
        assertPrints(10u.coerceIn(1u, 100u), "10")
        assertPrints(10u.coerceIn(1u..100u), "10")
        assertPrints(0u.coerceIn(1u, 100u), "1")
        assertPrints(500u.coerceIn(1u, 100u), "100")
        assertFailsWith<IllegalArgumentException> {
            10u.coerceIn(100u, 0u)
        }
    }

    @Sample
    fun coerceInComparable() {
        val workingDays = DayOfWeek.MONDAY..DayOfWeek.FRIDAY
        assertPrints(DayOfWeek.WEDNESDAY.coerceIn(workingDays), "WEDNESDAY")
        assertPrints(DayOfWeek.SATURDAY.coerceIn(workingDays), "FRIDAY")

        assertPrints(DayOfWeek.FRIDAY.coerceIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), "SATURDAY")
    }

    @Sample
    fun coerceInFloatingPointRange() {
        assertPrints(10.1.coerceIn(1.0..10.0), "10.0")
        assertPrints(9.9.coerceIn(1.0..10.0), "9.9")

        assertFailsWith<IllegalArgumentException> { 9.9.coerceIn(1.0..Double.NaN) }
    }
}