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

package samples.comparisons

import samples.*
import kotlin.test.*

class Comparisons {
    @Sample
    fun compareValuesByWithSingleSelector() {
        fun compareLength(a: String, b: String): Int =
            compareValuesBy(a, b) { it.length }

        assertTrue(compareLength("a", "b") == 0)
        assertTrue(compareLength("bb", "a") > 0)
        assertTrue(compareLength("a", "bb") < 0)
    }

    @Sample
    fun compareValuesByWithSelectors() {
        fun compareLengthThenString(a: String, b: String): Int =
            compareValuesBy(a, b, { it.length }, { it })

        assertTrue(compareLengthThenString("b", "aa") < 0)

        assertTrue(compareLengthThenString("a", "b") < 0)
        assertTrue(compareLengthThenString("b", "a") > 0)
        assertTrue(compareLengthThenString("a", "a") == 0)
    }

    @Sample
    fun compareValuesByWithComparator() {
        fun compareInsensitiveOrder(a: Char, b: Char): Int =
            compareValuesBy(a, b, String.CASE_INSENSITIVE_ORDER, { c -> c.toString() })

        assertTrue(compareInsensitiveOrder('a', 'a') == 0)
        assertTrue(compareInsensitiveOrder('a', 'A') == 0)

        assertTrue(compareInsensitiveOrder('a', 'b') < 0)
        assertTrue(compareInsensitiveOrder('A', 'b') < 0)
        assertTrue(compareInsensitiveOrder('b', 'a') > 0)
    }

    @Sample
    fun compareValues() {
        assertTrue(compareValues(null, 1) < 0)
        assertTrue(compareValues(1, 2) < 0)
        assertTrue(compareValues(2, 1) > 0)
        assertTrue(compareValues(1, 1) == 0)
    }

    @Sample
    fun compareByWithSingleSelector() {
        val list = listOf("aa", "b", "bb", "a")

        val sorted = list.sortedWith(compareBy { it.length })

        assertPrints(sorted, "[b, a, aa, bb]")
    }

    @Sample
    fun compareByWithSelectors() {
        val list = listOf("aa", "b", "bb", "a")

        val sorted = list.sortedWith(compareBy(
            { it.length },
            { it }
        ))

        assertPrints(sorted, "[a, b, aa, bb]")
    }

    @Sample
    fun compareByWithComparator() {
        val list = listOf('B', 'a', 'A', 'b')

        val sorted = list.sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER) { v -> v.toString() }
        )

        assertPrints(sorted, "[a, A, B, b]")
    }

    @Sample
    fun compareByDescendingWithSingleSelector() {
        val list = listOf("aa", "b", "bb", "a")

        val sorted = list.sortedWith(compareByDescending { it.length })

        assertPrints(sorted, "[aa, bb, b, a]")
    }

    @Sample
    fun compareByDescendingWithComparator() {
        val list = listOf('B', 'a', 'A', 'b')

        val sorted = list.sortedWith(
            compareByDescending(String.CASE_INSENSITIVE_ORDER) { v -> v.toString() }
        )

        assertPrints(sorted, "[B, b, a, A]")
    }

    @Sample
    fun thenBy() {
        val list = listOf("aa", "b", "bb", "a")

        val lengthComparator = compareBy<String> { it.length }
        assertPrints(list.sortedWith(lengthComparator), "[b, a, aa, bb]")

        val lengthThenString = lengthComparator.thenBy { it }
        assertPrints(list.sortedWith(lengthThenString), "[a, b, aa, bb]")
    }

    @Sample
    fun thenByWithComparator() {
        val list = listOf("A", "aa", "b", "bb", "a")

        val lengthComparator = compareBy<String> { it.length }
        assertPrints(list.sortedWith(lengthComparator), "[A, b, a, aa, bb]")

        val lengthThenCaseInsensitive = lengthComparator
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it }
        assertPrints(list.sortedWith(lengthThenCaseInsensitive), "[A, a, b, aa, bb]")
    }

    @Sample
    fun thenByDescending() {
        val list = listOf("aa", "b", "bb", "a")

        val lengthComparator = compareBy<String> { it.length }
        assertPrints(list.sortedWith(lengthComparator), "[b, a, aa, bb]")

        val lengthThenStringDesc = lengthComparator.thenByDescending { it }
        assertPrints(list.sortedWith(lengthThenStringDesc), "[b, a, bb, aa]")
    }

    @Sample
    fun thenByDescendingWithComparator() {
        val list = listOf("A", "aa", "b", "bb", "a")

        val lengthComparator = compareBy<String> { it.length }
        assertPrints(list.sortedWith(lengthComparator), "[A, b, a, aa, bb]")

        val lengthThenCaseInsensitive = lengthComparator
            .thenByDescending(String.CASE_INSENSITIVE_ORDER) { it }
        assertPrints(list.sortedWith(lengthThenCaseInsensitive), "[b, A, a, bb, aa]")
    }

    @Sample
    fun thenComparator() {
        val list = listOf("c" to 1, "b" to 2, "a" to 1, "d" to 0, null to 0)

        val valueComparator = compareBy<Pair<String?, Int>> { it.second }
        val map1 = list.sortedWith(valueComparator).toMap()
        assertPrints(map1, "{d=0, null=0, c=1, a=1, b=2}")

        val valueThenKeyComparator = valueComparator
            .thenComparator({ a, b -> compareValues(a.first, b.first) })
        val map2 = list.sortedWith(valueThenKeyComparator).toMap()
        assertPrints(map2, "{null=0, d=0, a=1, c=1, b=2}")
    }

    @Sample
    fun then() {
        val list = listOf("A", "aa", "b", "bb", "a")

        val lengthThenCaseInsensitive = compareBy<String> { it.length }
            .then(String.CASE_INSENSITIVE_ORDER)

        val sorted = list.sortedWith(lengthThenCaseInsensitive)

        assertPrints(sorted, "[A, a, b, aa, bb]")
    }

    @Sample
    fun thenDescending() {
        val list = listOf("A", "aa", "b", "bb", "a")

        val lengthThenCaseInsensitive = compareBy<String> { it.length }
            .thenDescending(String.CASE_INSENSITIVE_ORDER)

        val sorted = list.sortedWith(lengthThenCaseInsensitive)

        assertPrints(sorted, "[b, A, a, bb, aa]")
    }

    @Sample
    fun nullsFirstLastComparator() {
        val list = listOf(4, null, -1, 1)

        val nullsFirstList = list.sortedWith(nullsFirst())
        assertPrints(nullsFirstList, "[null, -1, 1, 4]")

        val nullsLastList = list.sortedWith(nullsLast())
        assertPrints(nullsLastList, "[-1, 1, 4, null]")
    }

    @Sample
    fun nullsFirstLastWithComparator() {
        val list = listOf(4, null, 1, -2, 3)

        val nullsFirstList = list.sortedWith(nullsFirst(reverseOrder()))
        assertPrints(nullsFirstList, "[null, 4, 3, 1, -2]")

        val nullsLastList = list.sortedWith(nullsLast(reverseOrder()))
        assertPrints(nullsLastList, "[4, 3, 1, -2, null]")
    }

    @Sample
    fun naturalOrderComparator() {
        val list = listOf("aa", "b", "bb", "a")

        val lengthThenNatural = compareBy<String> { it.length }
            .then(naturalOrder())

        val sorted = list.sortedWith(lengthThenNatural)

        assertPrints(sorted, "[a, b, aa, bb]")
    }

    @Sample
    fun reversed() {
        val list = listOf("aa", "b", "bb", "a")

        val lengthThenString = compareBy<String> { it.length }.thenBy { it }

        val sorted = list.sortedWith(lengthThenString)
        assertPrints(sorted, "[a, b, aa, bb]")

        val sortedReversed = list.sortedWith(lengthThenString.reversed())
        assertPrints(sortedReversed, "[bb, aa, b, a]")
    }
}