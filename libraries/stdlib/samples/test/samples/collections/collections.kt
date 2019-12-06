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

package samples.collections

import samples.*
import kotlin.test.*


@RunWith(Enclosed::class)
class Collections {

    class Collections {

        @Sample
        fun indicesOfCollection() {
            val empty = emptyList<Any>()
            assertTrue(empty.indices.isEmpty())
            val collection = listOf('a', 'b', 'c')
            assertPrints(collection.indices, "0..2")
        }

        @Sample
        fun collectionIsNotEmpty() {
            val empty = emptyList<Any>()
            assertFalse(empty.isNotEmpty())

            val collection = listOf('a', 'b', 'c')
            assertTrue(collection.isNotEmpty())
        }

        @Sample
        fun collectionOrEmpty() {
            val nullCollection: Collection<Any>? = null
            assertPrints(nullCollection.orEmpty(), "[]")

            val collection: Collection<Char>? = listOf('a', 'b', 'c')
            assertPrints(collection.orEmpty(), "[a, b, c]")
        }

        @Sample
        fun collectionIsNullOrEmpty() {
            val nullList: List<Any>? = null
            assertTrue(nullList.isNullOrEmpty())

            val empty: List<Any>? = emptyList<Any>()
            assertTrue(empty.isNullOrEmpty())

            val collection: List<Char>? = listOf('a', 'b', 'c')
            assertFalse(collection.isNullOrEmpty())
        }

        @Sample
        fun collectionIfEmpty() {
            val empty: List<Int> = emptyList()

            val emptyOrNull: List<Int>? = empty.ifEmpty { null }
            assertPrints(emptyOrNull, "null")

            val emptyOrDefault: List<Any> = empty.ifEmpty { listOf("default") }
            assertPrints(emptyOrDefault, "[default]")

            val nonEmpty = listOf("x")
            val sameList: List<String> = nonEmpty.ifEmpty { listOf("empty") }
            assertTrue(nonEmpty === sameList)
        }

        @Sample
        fun collectionContainsAll() {
            val collection = mutableListOf('a', 'b')
            val test = listOf('a', 'b', 'c')
            assertFalse(collection.containsAll(test))

            collection.add('c')
            assertTrue(collection.containsAll(test))
        }

        @Sample
        fun collectionToTypedArray() {
            val collection = listOf(1, 2, 3)
            val array = collection.toTypedArray()
            assertPrints(array.contentToString(), "[1, 2, 3]")
        }
    }

    class Lists {

        @Sample
        fun emptyReadOnlyList() {
            val list = listOf<String>()
            assertTrue(list.isEmpty())

            // another way to create an empty list,
            // type parameter is inferred from the expected type
            val other: List<Int> = emptyList()

            assertTrue(list == other, "Empty lists are equal")
            assertPrints(list, "[]")
            assertFails { list[0] }
        }

        @Sample
        fun readOnlyList() {
            val list = listOf('a', 'b', 'c')
            assertPrints(list.size, "3")
            assertTrue(list.contains('a'))
            assertPrints(list.indexOf('b'), "1")
            assertPrints(list[2], "c")
        }

        @Sample
        fun singletonReadOnlyList() {
            val list = listOf('a')
            assertPrints(list, "[a]")
            assertPrints(list.size, "1")
        }

        @Sample
        fun emptyMutableList() {
            val list = mutableListOf<Int>()
            assertTrue(list.isEmpty())

            list.addAll(listOf(1, 2, 3))
            assertPrints(list, "[1, 2, 3]")
        }

        @Sample
        fun emptyArrayList() {
            val list = arrayListOf<Int>()
            assertTrue(list.isEmpty())

            list.addAll(listOf(1, 2, 3))
            assertPrints(list, "[1, 2, 3]")
        }

        @Sample
        fun mutableList() {
            val list = mutableListOf(1, 2, 3)
            assertPrints(list, "[1, 2, 3]")

            list += listOf(4, 5)
            assertPrints(list, "[1, 2, 3, 4, 5]")
        }

        @Sample
        fun arrayList() {
            val list = arrayListOf(1, 2, 3)
            assertPrints(list, "[1, 2, 3]")

            list += listOf(4, 5)
            assertPrints(list, "[1, 2, 3, 4, 5]")
        }

        @Sample
        fun listOfNotNull() {
            val empty = listOfNotNull<Any>(null)
            assertPrints(empty, "[]")

            val singleton = listOfNotNull(42)
            assertPrints(singleton, "[42]")

            val list = listOfNotNull(1, null, 2, null, 3)
            assertPrints(list, "[1, 2, 3]")
        }

        @Sample
        fun readOnlyListFromInitializer() {
            val squares = List(5) { (it + 1) * (it + 1) }
            assertPrints(squares, "[1, 4, 9, 16, 25]")
        }

        @Sample
        fun mutableListFromInitializer() {
            val list = MutableList(3) { index -> 'A' + index }
            assertPrints(list, "[A, B, C]")

            list.clear()
            assertPrints(list, "[]")
        }

        @Sample
        fun lastIndexOfList() {
            assertPrints(emptyList<Any>().lastIndex, "-1")
            val list = listOf("a", "x", "y")
            assertPrints(list.lastIndex, "2")
            assertPrints(list[list.lastIndex], "y")
        }

        @Sample
        fun listOrEmpty() {
            val nullList: List<Any>? = null
            assertPrints(nullList.orEmpty(), "[]")

            val list: List<Char>? = listOf('a', 'b', 'c')
            assertPrints(list.orEmpty(), "[a, b, c]")
        }

        @Sample
        fun listFromEnumeration() {
            val numbers = java.util.Hashtable<String, Int>()
            numbers.put("one", 1)
            numbers.put("two", 2)
            numbers.put("three", 3)

            // when you have an Enumeration from some old code
            val enumeration: java.util.Enumeration<Int> = numbers.elements()

            // you can convert it to list and transform further with list operations
            val list = enumeration.toList().sorted()
            assertPrints(list, "[1, 2, 3]")
        }

        @Sample
        fun binarySearchOnComparable() {
            val list = mutableListOf('a', 'b', 'c', 'd', 'e')
            assertPrints(list.binarySearch('d'), "3")

            list.remove('d')

            val invertedInsertionPoint = list.binarySearch('d')
            val actualInsertionPoint = -(invertedInsertionPoint + 1)
            assertPrints(actualInsertionPoint, "3")

            list.add(actualInsertionPoint, 'd')
            assertPrints(list, "[a, b, c, d, e]")
        }

        @Sample
        fun binarySearchWithBoundaries() {
            val list = listOf('a', 'b', 'c', 'd', 'e')
            assertPrints(list.binarySearch('d'), "3")

            // element is out of range from the left
            assertTrue(list.binarySearch('b', fromIndex = 2) < 0)

            // element is out of range from the right
            assertTrue(list.binarySearch('d', toIndex = 2) < 0)
        }

        @Sample
        fun binarySearchWithComparator() {
            val colors = listOf("Blue", "green", "ORANGE", "Red", "yellow")
            assertPrints(colors.binarySearch("RED", String.CASE_INSENSITIVE_ORDER), "3")
        }

        @Sample
        fun binarySearchByKey() {
            data class Box(val value: Int)

            val numbers = listOf(1, 3, 7, 10, 12)
            val boxes = numbers.map { Box(it) }
            assertPrints(boxes.binarySearchBy(10) { it.value }, "3")
        }

        @Sample
        fun binarySearchWithComparisonFunction() {
            data class Box(val value: String)

            val values = listOf("A", "ant", "binding", "Box", "cell")
            val boxes = values.map { Box(it) }

            val valueToFind = "box"
            // `boxes` list is sorted according to the following comparison function
            val index = boxes.binarySearch { String.CASE_INSENSITIVE_ORDER.compare(it.value, valueToFind) }

            if (index >= 0) {
                assertPrints("Value at $index is ${boxes[index]}", "Value at 3 is Box(value=Box)")
            } else {
                println("Box with value=$valueToFind was not found")
            }
        }
    }

    class Sets {

        @Sample
        fun emptyReadOnlySet() {
            val set = setOf<String>()
            assertTrue(set.isEmpty())

            // another way to create an empty set,
            // type parameter is inferred from the expected type
            val other: Set<Int> = emptySet()

            assertTrue(set == other, "Empty sets are equal")
            assertPrints(set, "[]")
        }

        @Sample
        fun readOnlySet() {
            val set1 = setOf(1, 2, 3)
            val set2 = setOf(3, 2, 1)

            // setOf preserves the iteration order of elements
            assertPrints(set1, "[1, 2, 3]")
            assertPrints(set2, "[3, 2, 1]")

            // but the sets with the same elements are equal no matter of order
            assertTrue(set1 == set2)
        }

        @Sample
        fun emptyMutableSet() {
            val set = mutableSetOf<Int>()
            assertTrue(set.isEmpty())

            set.add(1)
            set.add(2)
            set.add(1)

            assertPrints(set, "[1, 2]")
        }

        @Sample
        fun mutableSet() {
            val set = mutableSetOf(1, 2, 3)
            assertPrints(set, "[1, 2, 3]")

            set.remove(3)
            set += listOf(4, 5)
            assertPrints(set, "[1, 2, 4, 5]")
        }
    }

    class Transformations {

        @Sample
        fun associate() {
            val names = listOf("Grace Hopper", "Jacob Bernoulli", "Johann Bernoulli")

            val byLastName = names.associate { it.split(" ").let { (firstName, lastName) -> lastName to firstName } }

            // Jacob Bernoulli does not occur in the map because only the last pair with the same key gets added
            assertPrints(byLastName, "{Hopper=Grace, Bernoulli=Johann}")
        }

        @Sample
        fun associateBy() {
            data class Person(val firstName: String, val lastName: String) {
                override fun toString(): String = "$firstName $lastName"
            }

            val scientists = listOf(Person("Grace", "Hopper"), Person("Jacob", "Bernoulli"), Person("Johann", "Bernoulli"))

            val byLastName = scientists.associateBy { it.lastName }

            // Jacob Bernoulli does not occur in the map because only the last pair with the same key gets added
            assertPrints(byLastName, "{Hopper=Grace Hopper, Bernoulli=Johann Bernoulli}")
        }

        @Sample
        fun associateByWithValueTransform() {
            data class Person(val firstName: String, val lastName: String)

            val scientists = listOf(Person("Grace", "Hopper"), Person("Jacob", "Bernoulli"), Person("Johann", "Bernoulli"))

            val byLastName = scientists.associateBy({ it.lastName }, { it.firstName })

            // Jacob Bernoulli does not occur in the map because only the last pair with the same key gets added
            assertPrints(byLastName, "{Hopper=Grace, Bernoulli=Johann}")
        }

        @Sample
        fun associateByTo() {
            data class Person(val firstName: String, val lastName: String) {
                override fun toString(): String = "$firstName $lastName"
            }

            val scientists = listOf(Person("Grace", "Hopper"), Person("Jacob", "Bernoulli"), Person("Johann", "Bernoulli"))

            val byLastName = mutableMapOf<String, Person>()
            assertTrue(byLastName.isEmpty())

            scientists.associateByTo(byLastName) { it.lastName }

            assertTrue(byLastName.isNotEmpty())
            // Jacob Bernoulli does not occur in the map because only the last pair with the same key gets added
            assertPrints(byLastName, "{Hopper=Grace Hopper, Bernoulli=Johann Bernoulli}")
        }

        @Sample
        fun associateByToWithValueTransform() {
            data class Person(val firstName: String, val lastName: String)

            val scientists = listOf(Person("Grace", "Hopper"), Person("Jacob", "Bernoulli"), Person("Johann", "Bernoulli"))

            val byLastName = mutableMapOf<String, String>()
            assertTrue(byLastName.isEmpty())

            scientists.associateByTo(byLastName, { it.lastName }, { it.firstName} )

            assertTrue(byLastName.isNotEmpty())
            // Jacob Bernoulli does not occur in the map because only the last pair with the same key gets added
            assertPrints(byLastName, "{Hopper=Grace, Bernoulli=Johann}")
        }

        @Sample
        fun associateTo() {
            data class Person(val firstName: String, val lastName: String)

            val scientists = listOf(Person("Grace", "Hopper"), Person("Jacob", "Bernoulli"), Person("Johann", "Bernoulli"))

            val byLastName = mutableMapOf<String, String>()
            assertTrue(byLastName.isEmpty())

            scientists.associateTo(byLastName) { it.lastName to it.firstName }

            assertTrue(byLastName.isNotEmpty())
            // Jacob Bernoulli does not occur in the map because only the last pair with the same key gets added
            assertPrints(byLastName, "{Hopper=Grace, Bernoulli=Johann}")
        }

        @Sample
        fun associateWith() {
            val words = listOf("a", "abc", "ab", "def", "abcd")
            val withLength = words.associateWith { it.length }
            assertPrints(withLength.keys, "[a, abc, ab, def, abcd]")
            assertPrints(withLength.values, "[1, 3, 2, 3, 4]")
        }

        @Sample
        fun associateWithTo() {
            data class Person(val firstName: String, val lastName: String) {
                override fun toString(): String = "$firstName $lastName"
            }

            val scientists = listOf(Person("Grace", "Hopper"), Person("Jacob", "Bernoulli"), Person("Jacob", "Bernoulli"))
            val withLengthOfNames = mutableMapOf<Person, Int>()
            assertTrue(withLengthOfNames.isEmpty())

            scientists.associateWithTo(withLengthOfNames) { it.firstName.length + it.lastName.length }

            assertTrue(withLengthOfNames.isNotEmpty())
            // Jacob Bernoulli only occurs once in the map because only the last pair with the same key gets added
            assertPrints(withLengthOfNames, "{Grace Hopper=11, Jacob Bernoulli=14}")
        }

        @Sample
        fun distinctAndDistinctBy() {
            val list = listOf('a', 'A', 'b', 'B', 'A', 'a')
            assertPrints(list.distinct(), "[a, A, b, B]")
            assertPrints(list.distinctBy { it.toUpperCase() }, "[a, b]")
        }

        @Sample
        fun groupBy() {
            val words = listOf("a", "abc", "ab", "def", "abcd")
            val byLength = words.groupBy { it.length }

            assertPrints(byLength.keys, "[1, 3, 2, 4]")
            assertPrints(byLength.values, "[[a], [abc, def], [ab], [abcd]]")

            val mutableByLength: MutableMap<Int, MutableList<String>> = words.groupByTo(mutableMapOf()) { it.length }
            // same content as in byLength map, but the map is mutable
            assertTrue(mutableByLength == byLength)
        }

        @Sample
        fun groupByKeysAndValues() {
            val nameToTeam = listOf("Alice" to "Marketing", "Bob" to "Sales", "Carol" to "Marketing")
            val namesByTeam = nameToTeam.groupBy({ it.second }, { it.first })
            assertPrints(namesByTeam, "{Marketing=[Alice, Carol], Sales=[Bob]}")

            val mutableNamesByTeam = nameToTeam.groupByTo(HashMap(), { it.second }, { it.first })
            // same content as in namesByTeam map, but the map is mutable
            assertTrue(mutableNamesByTeam == namesByTeam)
        }



        @Sample
        fun joinTo() {
            val sb = StringBuilder("An existing string and a list: ")
            val numbers = listOf(1, 2, 3)
            assertPrints(numbers.joinTo(sb, prefix = "[", postfix = "]").toString(), "An existing string and a list: [1, 2, 3]")

            val lotOfNumbers: Iterable<Int> = 1..100
            val firstNumbers = StringBuilder("First five numbers: ")
            assertPrints(lotOfNumbers.joinTo(firstNumbers, limit = 5).toString(), "First five numbers: 1, 2, 3, 4, 5, ...")
        }

        @Sample
        fun joinToString() {
            val numbers = listOf(1, 2, 3, 4, 5, 6)
            assertPrints(numbers.joinToString(), "1, 2, 3, 4, 5, 6")
            assertPrints(numbers.joinToString(prefix = "[", postfix = "]"), "[1, 2, 3, 4, 5, 6]")
            assertPrints(numbers.joinToString(prefix = "<", postfix = ">", separator = "•"), "<1•2•3•4•5•6>")

            val chars = charArrayOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q')
            assertPrints(chars.joinToString(limit = 5, truncated = "...!") { it.toUpperCase().toString() }, "A, B, C, D, E, ...!")
        }

        @Sample
        fun map() {
            val numbers = listOf(1, 2, 3)
            assertPrints(numbers.map { it * it }, "[1, 4, 9]")
        }

        @Sample
        fun flatMap() {
            val list = listOf("123", "45")
            assertPrints(list.flatMap { it.toList() }, "[1, 2, 3, 4, 5]")
        }

        @Sample
        fun take() {
            val chars = ('a'..'z').toList()
            assertPrints(chars.take(3), "[a, b, c]")
            assertPrints(chars.takeWhile { it < 'f' }, "[a, b, c, d, e]")
            assertPrints(chars.takeLast(2), "[y, z]")
            assertPrints(chars.takeLastWhile { it > 'w' }, "[x, y, z]")
        }

        @Sample
        fun drop() {
            val chars = ('a'..'z').toList()
            assertPrints(chars.drop(23), "[x, y, z]")
            assertPrints(chars.dropLast(23), "[a, b, c]")
            assertPrints(chars.dropWhile { it < 'x' }, "[x, y, z]")
            assertPrints(chars.dropLastWhile { it > 'c' }, "[a, b, c]")
        }

        @Sample
        fun chunked() {
            val words = "one two three four five six seven eight nine ten".split(' ')
            val chunks = words.chunked(3)

            assertPrints(chunks, "[[one, two, three], [four, five, six], [seven, eight, nine], [ten]]")
        }

        @Sample
        fun zipWithNext() {
            val letters = ('a'..'f').toList()
            val pairs = letters.zipWithNext()

            assertPrints(letters, "[a, b, c, d, e, f]")
            assertPrints(pairs, "[(a, b), (b, c), (c, d), (d, e), (e, f)]")
        }

        @Sample
        fun zipWithNextToFindDeltas() {
            val values = listOf(1, 4, 9, 16, 25, 36)
            val deltas = values.zipWithNext { a, b -> b - a }

            assertPrints(deltas, "[3, 5, 7, 9, 11]")
        }
    }

    class Aggregates {
        @Sample
        fun all() {
            val isEven: (Int) -> Boolean = { it % 2 == 0 }
            val zeroToTen = 0..10
            assertFalse(zeroToTen.all { isEven(it) })
            assertFalse(zeroToTen.all(isEven))

            val evens = zeroToTen.map { it * 2 }
            assertTrue(evens.all { isEven(it) })

            val emptyList = emptyList<Int>()
            assertTrue(emptyList.all { false })
        }

        @Sample
        fun none() {
            val emptyList = emptyList<Int>()
            assertTrue(emptyList.none())

            val nonEmptyList = listOf("one", "two", "three")
            assertFalse(nonEmptyList.none())
        }

        @Sample
        fun noneWithPredicate() {
            val isEven: (Int) -> Boolean = { it % 2 == 0 }
            val zeroToTen = 0..10
            assertFalse(zeroToTen.none { isEven(it) })
            assertFalse(zeroToTen.none(isEven))

            val odds = zeroToTen.map { it * 2 + 1 }
            assertTrue(odds.none { isEven(it) })

            val emptyList = emptyList<Int>()
            assertTrue(emptyList.none { true })
        }

        @Sample
        fun any() {
            val emptyList = emptyList<Int>()
            assertFalse(emptyList.any())

            val nonEmptyList = listOf(1, 2, 3)
            assertTrue(nonEmptyList.any())
        }

        @Sample
        fun anyWithPredicate() {
            val isEven: (Int) -> Boolean = { it % 2 == 0 }
            val zeroToTen = 0..10
            assertTrue(zeroToTen.any { isEven(it) })
            assertTrue(zeroToTen.any(isEven))

            val odds = zeroToTen.map { it * 2 + 1 }
            assertFalse(odds.any { isEven(it) })

            val emptyList = emptyList<Int>()
            assertFalse(emptyList.any { true })
        }

        @Sample
        fun maxBy() {
            val nameToAge = listOf("Alice" to 42, "Bob" to 28, "Carol" to 51)
            val oldestPerson = nameToAge.maxBy { it.second }
            assertPrints(oldestPerson, "(Carol, 51)")

            val emptyList = emptyList<Pair<String, Int>>()
            val emptyMax = emptyList.maxBy { it.second }
            assertPrints(emptyMax, "null")
        }

        @Sample
        fun minBy() {
            val list = listOf("abcd", "abc", "ab", "abcde")
            val shortestString = list.minBy { it.length }
            assertPrints(shortestString, "ab")

            val emptyList = emptyList<String>()
            val emptyMin = emptyList.minBy { it.length }
            assertPrints(emptyMin, "null")
        }

        @Sample
        fun reduce() {
            val strings = listOf("a", "b", "c", "d")
            assertPrints(strings.reduce { acc, string -> acc + string }, "abcd")
            assertPrints(strings.reduceIndexed { index, acc, string -> acc + string + index }, "ab1c2d3")

            assertFails { emptyList<Int>().reduce { _, _ -> 0 } }
        }

        @Sample
        fun reduceRight() {
            val strings = listOf("a", "b", "c", "d")
            assertPrints(strings.reduceRight { string, acc -> acc + string }, "dcba")
            assertPrints(strings.reduceRightIndexed { index, string, acc -> acc + string + index }, "dc2b1a0")

            assertFails { emptyList<Int>().reduceRight { _, _ -> 0 } }
        }

        @Sample
        fun reduceOrNull() {
            val strings = listOf("a", "b", "c", "d")
            assertPrints(strings.reduceOrNull { acc, string -> acc + string }, "abcd")

            assertPrints(emptyList<String>().reduceOrNull { _, _ -> "" }, "null")
        }

        @Sample
        fun reduceRightOrNull() {
            val strings = listOf("a", "b", "c", "d")
            assertPrints(strings.reduceRightOrNull { string, acc -> acc + string }, "dcba")

            assertPrints(emptyList<String>().reduceRightOrNull { _, _ -> "" }, "null")
        }
    }

    class Elements {
        @Sample
        fun elementAt() {
            val list = listOf(1, 2, 3)
            assertPrints(list.elementAt(0), "1")
            assertPrints(list.elementAt(2), "3")
            assertFailsWith<IndexOutOfBoundsException> { list.elementAt(3) }

            val emptyList = emptyList<Int>()
            assertFailsWith<IndexOutOfBoundsException> { emptyList.elementAt(0) }
        }

        @Sample
        fun elementAtOrNull() {
            val list = listOf(1, 2, 3)
            assertPrints(list.elementAtOrNull(0), "1")
            assertPrints(list.elementAtOrNull(2), "3")
            assertPrints(list.elementAtOrNull(3), "null")

            val emptyList = emptyList<Int>()
            assertPrints(emptyList.elementAtOrNull(0), "null")
        }

        @Sample
        fun elementAtOrElse() {
            val list = listOf(1, 2, 3)
            assertPrints(list.elementAtOrElse(0) { 42 }, "1")
            assertPrints(list.elementAtOrElse(2) { 42 }, "3")
            assertPrints(list.elementAtOrElse(3) { 42 }, "42")

            val emptyList = emptyList<Int>()
            assertPrints(emptyList.elementAtOrElse(0) { "no int" }, "no int")
        }
    }

    class Sorting {

        @Sample
        fun sortMutableList() {
            val mutableList = mutableListOf(4, 3, 2, 1)

            // before sorting
            assertPrints(mutableList.joinToString(), "4, 3, 2, 1")

            mutableList.sort()

            // after sorting
            assertPrints(mutableList.joinToString(), "1, 2, 3, 4")
        }

        @Sample
        fun sortMutableListWith() {
            // non-comparable class
            class Person(val firstName: String, val lastName: String) {
                override fun toString(): String = "$firstName $lastName"
            }

            val people = mutableListOf(
                Person("Ragnar", "Lodbrok"),
                Person("Bjorn", "Ironside"),
                Person("Sweyn", "Forkbeard")
            )

            people.sortWith(compareByDescending { it.firstName })

            // after sorting
            assertPrints(people.joinToString(), "Sweyn Forkbeard, Ragnar Lodbrok, Bjorn Ironside")
        }

    }
}