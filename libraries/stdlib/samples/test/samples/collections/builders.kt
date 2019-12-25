package samples.collections

import samples.*
import java.util.*
import kotlin.math.log2
import kotlin.test.*

@Suppress("ReplaceAssertBooleanWithAssertEquality")
@RunWith(Enclosed::class)
class Builders {
    class Lists {
        @Sample
        fun buildArrayList() {
            val x = arrayList {
                add('a')
                add('b')
            }
            assertPrints(x, "[a, b]")

            val y = arrayList(initialCapacity = x.size + 1) {
                addAll(x)
                add('c')
            }
            assertPrints(y, "[a, b, c]")

            assertFails("initial capacity cannot be negative") {
                arrayList(-1) {
                    add('a')
                }
            }
        }

        @Sample
        fun buildArrayListOfNonNull() {
            val x = arrayListOfNonNull<Char> {
                add('a')
                add(null)
                add('b')
                add(null)
            }
            assertTrue(x == arrayListOf('a', 'b'))
            assertPrints(x, "[a, b]")

            val y = arrayListOfNonNull<Char>(initialCapacity = x.size + 1) {
                addAll(x)
                add('c')
            }
            assertTrue(y == arrayListOf('a', 'b', 'c'))
            assertPrints(y, "[a, b, c]")
        }

        @Sample
        fun buildMutableList() {
            val x = mutableList {
                add('a')
                add('b')
            }
            assertPrints(x, "[a, b]")

            val y = mutableList(initialCapacity = x.size + 1) {
                addAll(x)
                add('c')
            }
            assertPrints(y, "[a, b, c]")

            assertFails("initial capacity cannot be negative") {
                mutableList(-1) {
                    add('a')
                }
            }
        }

        @Sample
        fun buildMutableListOfNonNull() {
            val x = mutableListOfNonNull<Char> {
                add('a')
                add(null)
                add('b')
                add(null)
            }
            assertTrue(x == mutableListOf('a', 'b'))
            assertPrints(x, "[a, b]")

            val y = mutableListOfNonNull<Char>(initialCapacity = x.size + 1) {
                addAll(x)
                add('c')
            }
            assertTrue(y == mutableListOf('a', 'b', 'c'))
            assertPrints(y, "[a, b, c]")
        }

        @Sample
        fun buildList() {
            val x = list {
                add('a')
                add('b')
            }
            assertPrints(x, "[a, b]")

            val y = list(initialCapacity = x.size + 1) {
                addAll(x)
                add('c')
            }
            assertPrints(y, "[a, b, c]")

            assertFails("initial capacity cannot be negative") {
                list(-1) {
                    add('a')
                }
            }
        }

        @Sample
        fun buildListOfNonNull() {
            val x = listOfNonNull<Char> {
                add('a')
                add(null)
                add('b')
                add(null)
            }
            assertTrue(x == listOf('a', 'b'))
            assertPrints(x, "[a, b]")

            val y = listOfNonNull<Char>(initialCapacity = x.size + 1) {
                addAll(x)
                add('c')
            }
            assertTrue(y == listOf('a', 'b', 'c'))
            assertPrints(y, "[a, b, c]")
        }
    }

    class Sets {
        @Sample
        fun buildHashSet() {
            val x = hashSet {
                add('a')
                add('b')
            }
            assertPrints(x.sorted(), "[a, b]")

            val y = hashSet(initialCapacity = x.size + 1) {
                addAll(x)
                add('c')
            }
            assertPrints(y.sorted(), "[a, b, c]")

            assertFails("initial capacity cannot be negative") {
                hashSet(-1) {
                    add('a')
                }
            }

            val z = hashSet(y.size + 1, loadFactor = log2(2f)) {
                addAll(y)
                add('d')
            }
            assertPrints(z.sorted(), "[a, b, c, d]")

            assertFails("load factor must be greater zero") {
                hashSet(16, 0f) {
                    add('a')
                }
            }
        }

        @Sample
        fun buildLinkedSet() {
            val x = linkedSet {
                add('a')
                add('b')
            }
            assertPrints(x.sorted(), "[a, b]")

            val y = linkedSet(initialCapacity = x.size + 1) {
                addAll(x)
                add('c')
            }
            assertPrints(y.sorted(), "[a, b, c]")

            assertFails("initial capacity cannot be negative") {
                linkedSet(-1) {
                    add('a')
                }
            }

            val z = linkedSet(y.size + 1, loadFactor = log2(2f)) {
                addAll(y)
                add('d')
            }
            assertPrints(z.sorted(), "[a, b, c, d]")

            assertFails("load factor must be greater zero") {
                linkedSet(16, 0f) {
                    add('a')
                }
            }
        }

        @Sample
        fun buildMutableSet() {
            val x = mutableSet {
                add('a')
                add('b')
            }
            assertPrints(x.sorted(), "[a, b]")

            val y = mutableSet(initialCapacity = x.size + 1) {
                addAll(x)
                add('c')
            }
            assertPrints(y.sorted(), "[a, b, c]")

            assertFails("initial capacity cannot be negative") {
                mutableSet(-1) {
                    add('a')
                }
            }

            val z = mutableSet(y.size + 1, loadFactor = log2(2f)) {
                addAll(y)
                add('d')
            }
            assertPrints(z.sorted(), "[a, b, c, d]")

            assertFails("load factor must be greater zero") {
                mutableSet(16, 0f) {
                    add('a')
                }
            }
        }

        @Sample
        fun buildSet() {
            val x = set {
                add('a')
                add('b')
            }
            assertPrints(x.sorted(), "[a, b]")

            val y = set(initialCapacity = x.size + 1) {
                addAll(x)
                add('c')
            }
            assertPrints(y.sorted(), "[a, b, c]")

            assertFails("initial capacity cannot be negative") {
                set(-1) {
                    add('a')
                }
            }

            val z = set(y.size + 1, loadFactor = log2(2f)) {
                addAll(y)
                add('d')
            }
            assertPrints(z.sorted(), "[a, b, c, d]")

            assertFails("load factor must be greater zero") {
                set(16, 0f) {
                    add('a')
                }
            }
        }

        @Sample
        fun buildSortedSet() {
            val w = sortedSet {
                add('b')
                add('a')
            }
            assertPrints(w, "[a, b]")

            val x = sortedSet(Comparator.nullsFirst(Char::compareTo)) {
                add('b')
                add(null)
                add('a')
            }
            assertPrints(x, "[null, a, b]")

            val y = sortedSet(
                { lhs, rhs ->
                    when {
                        lhs == null && rhs == null -> 0
                        lhs == null -> 1
                        rhs == null -> -1
                        else -> lhs.compareTo(rhs)
                    }
                }
            ) {
                add('c')
                add(null)
                add('b')
            }
            assertPrints(y, "[b, c, null]")

            // uses comparator from y
            val z = sortedSet(y) {
                add('a')
            }
            assertPrints(z, "[a, b, c, null]")
        }
    }

    class Maps {
        @Sample
        fun buildHashMap() {
            val x = hashMap<Char, Int> {
                put('a', 1)
                put('b', 2)
            }
            assertPrints(x.toSortedMap(), "{a=1, b=2}")

            val y = hashMap<Char, Int>(initialCapacity = x.size + 1) {
                putAll(x)
                put('c', 3)
            }
            assertPrints(y.toSortedMap(), "{a=1, b=2, c=3}")

            assertFails("initial capacity cannot be negative") {
                hashMap<Char, Int>(-1) {
                    put('a', 1)
                }
            }

            val z = hashMap<Char, Int>(y.size + 1, loadFactor = log2(2f)) {
                putAll(y)
                put('d', 4)
            }
            assertPrints(z.toSortedMap(), "{a=1, b=2, c=3, d=4}")

            assertFails("load factor must be greater zero") {
                hashMap<Char, Int>(16, 0f) {
                    put('a', 1)
                }
            }
        }

        @Sample
        fun buildLinkedMap() {
            val x = linkedMap<Char, Int> {
                put('a', 1)
                put('b', 2)
            }
            assertPrints(x.toSortedMap(), "{a=1, b=2}")

            val y = linkedMap<Char, Int>(initialCapacity = x.size + 1) {
                putAll(x)
                put('c', 3)
            }
            assertPrints(y.toSortedMap(), "{a=1, b=2, c=3}")

            assertFails("initial capacity cannot be negative") {
                linkedMap<Char, Int>(-1) {
                    put('a', 1)
                }
            }

            val z = linkedMap<Char, Int>(y.size + 1, loadFactor = log2(2f)) {
                putAll(y)
                put('d', 4)
            }
            assertPrints(z.toSortedMap(), "{a=1, b=2, c=3, d=4}")

            assertFails("load factor must be greater zero") {
                linkedMap<Char, Int>(16, 0f) {
                    put('a', 1)
                }
            }
        }

        @Sample
        fun buildMutableMap() {
            val x = mutableMap<Char, Int> {
                put('a', 1)
                put('b', 2)
            }
            assertPrints(x.toSortedMap(), "{a=1, b=2}")

            val y = mutableMap<Char, Int>(initialCapacity = x.size + 1) {
                putAll(x)
                put('c', 3)
            }
            assertPrints(y.toSortedMap(), "{a=1, b=2, c=3}")

            assertFails("initial capacity cannot be negative") {
                mutableMap<Char, Int>(-1) {
                    put('a', 1)
                }
            }

            val z = mutableMap<Char, Int>(y.size + 1, loadFactor = log2(2f)) {
                putAll(y)
                put('d', 4)
            }
            assertPrints(z.toSortedMap(), "{a=1, b=2, c=3, d=4}")

            assertFails("load factor must be greater zero") {
                mutableMap<Char, Int>(16, 0f) {
                    put('a', 1)
                }
            }
        }

        @Sample
        fun buildMap() {
            val x = map<Char, Int> {
                put('a', 1)
                put('b', 2)
            }
            assertPrints(x.toSortedMap(), "{a=1, b=2}")

            val y = map<Char, Int>(initialCapacity = x.size + 1) {
                putAll(x)
                put('c', 3)
            }
            assertPrints(y.toSortedMap(), "{a=1, b=2, c=3}")

            assertFails("initial capacity cannot be negative") {
                map<Char, Int>(-1) {
                    put('a', 1)
                }
            }

            val z = map<Char, Int>(y.size + 1, loadFactor = log2(2f)) {
                putAll(y)
                put('d', 4)
            }
            assertPrints(z.toSortedMap(), "{a=1, b=2, c=3, d=4}")

            assertFails("load factor must be greater zero") {
                map<Char, Int>(16, 0f) {
                    put('a', 1)
                }
            }
        }

        @Sample
        fun buildSortedMap() {
            val w = sortedMap<Char, Int> {
                put('b', 2)
                put('a', 1)
            }
            assertPrints(w, "{a=1, b=2}")

            val x = sortedMap<Char?, Int>(Comparator.nullsFirst { lhs, rhs -> lhs!!.compareTo(rhs!!) }) {
                put('b', 2)
                put(null, 0)
                put('a', 1)
            }
            assertPrints(x, "{null=0, a=1, b=2}")

            val y = sortedMap<Char?, Int>(
                { lhs, rhs ->
                    when {
                        lhs == null && rhs == null -> 0
                        lhs == null -> 1
                        rhs == null -> -1
                        else -> lhs.compareTo(rhs)
                    }
                }
            ) {
                put('c', 3)
                put(null, 0)
                put('b', 2)
            }
            assertPrints(y, "{b=2, c=3, null=0}")

            // uses comparator from y
            val z = sortedMap(y) {
                put('a', 1)
            }
            assertPrints(z, "{a=1, b=2, c=3, null=0}")
        }
    }
}
