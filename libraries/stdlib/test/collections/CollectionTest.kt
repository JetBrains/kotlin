/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import test.assertStaticAndRuntimeTypeIs
import kotlin.test.*
import test.collections.behaviors.*
import test.comparisons.STRING_CASE_INSENSITIVE_ORDER
import kotlin.random.Random

class CollectionTest {

    @Test fun createListWithInit() {
        val list = List(3) { index -> "x".repeat(index + 1) }
        assertEquals(3, list.size)
        assertEquals(listOf("x", "xx", "xxx"), list)
    }

    @Test fun joinTo() {
        val data = listOf("foo", "bar")
        val buffer = StringBuilder()
        data.joinTo(buffer, "-", "{", "}")
        assertEquals("{foo-bar}", buffer.toString())
    }

    @Test fun joinToString() {
        val data = listOf("foo", "bar")
        val text = data.joinToString("-", "<", ">")
        assertEquals("<foo-bar>", text)

        val mixed = listOf('a', "b", StringBuilder("c"), null, "d", 'e', 'f')
        val text2 = mixed.joinToString(limit = 4, truncated = "*")
        assertEquals("a, b, c, null, *", text2)
    }

    @Test fun filterNotNull() {
        val data = listOf(null, "foo", null, "bar")
        val foo = data.filterNotNull()

        assertEquals(2, foo.size)
        assertEquals(listOf("foo", "bar"), foo)

        assertStaticAndRuntimeTypeIs<List<String>>(foo)
    }

    /*
    @Test fun mapNotNull() {
        val data = listOf(null, "foo", null, "bar")
        val foo = data.mapNotNull { it.length() }
        assertEquals(2, foo.size())
        assertEquals(listOf(3, 3), foo)

        assertTrue {
            foo is List<Int>
        }
    }
    */

    @Test fun listOfNotNull() {
        val l1: List<Int> = listOfNotNull(null)
        assertTrue(l1.isEmpty())

        val s: String? = "value"
        val l2: List<String> = listOfNotNull(s)
        assertEquals(s, l2.single())

        val l3: List<String> = listOfNotNull("value1", null, "value2")
        assertEquals(listOf("value1", "value2"), l3)
    }

    @Test fun filterIntoSet() {
        val data = listOf("foo", "bar")
        val foo = data.filterTo(hashSetOf<String>()) { it.startsWith("f") }

        assertTrue {
            foo.all { it.startsWith("f") }
        }
        assertEquals(1, foo.size)
        assertEquals(hashSetOf("foo"), foo)

        assertStaticAndRuntimeTypeIs<HashSet<String>>(foo)
    }

    @Test fun filterIsInstanceList() {
        val values: List<Any> = listOf(1, 2, 3.0, "abc", "cde")

        val numberValues: List<Number> = values.filterIsInstance<Number>()
        assertEquals(listOf(1, 2, 3.0), numberValues)

        // doesn't distinguish double from int in JS
//        val doubleValues: List<Double> = values.filterIsInstance<Double>()
//        assertEquals(listOf(3.0), doubleValues)

        val stringValues: List<String> = values.filterIsInstance<String>()
        assertEquals(listOf("abc", "cde"), stringValues)

        // is Any doesn't work in JS, see KT-7665
//        val anyValues: List<Any> = values.filterIsInstance<Any>()
//        assertEquals(values.toList(), anyValues)

        val charValues: List<Char> = values.filterIsInstance<Char>()
        assertEquals(0, charValues.size)
    }

    @Test fun filterIsInstanceArray() {
        val src: Array<Any> = arrayOf(1, 2, 3.0, "abc", "cde")

        val numberValues: List<Number> = src.filterIsInstance<Number>()
        assertEquals(listOf(1, 2, 3.0), numberValues)

        // doesn't distinguish double from int in JS
//        val doubleValues: List<Double> = src.filterIsInstance<Double>()
//        assertEquals(listOf(3.0), doubleValues)

        val stringValues: List<String> = src.filterIsInstance<String>()
        assertEquals(listOf("abc", "cde"), stringValues)

        // is Any doesn't work in JS, see KT-7665
//        val anyValues: List<Any> = src.filterIsInstance<Any>()
//        assertEquals(src.toList(), anyValues)

        val charValues: List<Char> = src.filterIsInstance<Char>()
        assertEquals(0, charValues.size)
    }

    @Test fun foldIndexed() {
        expect(42) {
            val numbers = listOf(1, 2, 3, 4)
            numbers.foldIndexed(0) { index, a, b -> index * (a + b) }
        }

        expect(0) {
            val numbers = arrayListOf<Int>()
            numbers.foldIndexed(0) { index, a, b -> index * (a + b) }
        }

        expect("11234") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.map { it.toString() }.foldIndexed("") { index, a, b -> if (index == 0) a + b + b else a + b }
        }
    }

    @Test fun foldIndexedWithDifferentTypes() {
        expect(10) {
            val numbers = listOf("a", "ab", "abc")
            numbers.foldIndexed(1) { index, a, b -> a + b.length + index }
        }

        expect("11223344") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.foldIndexed("") { index, a, b -> a + b + (index + 1) }
        }
    }

    @Test fun foldIndexedWithNonCommutativeOperation() {
        expect(4) {
            val numbers = listOf(1, 2, 3)
            numbers.foldIndexed(7) { index, a, b -> index + a - b }
        }
    }

    @Test fun foldRightIndexed() {
        expect("12343210") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.map { it.toString() }.foldRightIndexed("") { index, a, b -> a + b + index }
        }
    }

    @Test fun foldRightIndexedWithDifferentTypes() {
        expect("12343210") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.foldRightIndexed("") { index, a, b -> "" + a + b + index }
        }
    }

    @Test fun foldRightIndexedWithNonCommutativeOperation() {
        expect(-4) {
            val numbers = listOf(1, 2, 3)
            numbers.foldRightIndexed(7) { index, a, b -> index + a - b }
        }
    }

    @Test fun fold() {
        // lets calculate the sum of some numbers
        expect(10) {
            val numbers = listOf(1, 2, 3, 4)
            numbers.fold(0) { a, b -> a + b }
        }

        expect(0) {
            val numbers = arrayListOf<Int>()
            numbers.fold(0) { a, b -> a + b }
        }

        // lets concatenate some strings
        expect("1234") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.map { it.toString() }.fold("") { a, b -> a + b }
        }
    }

    @Test fun foldWithDifferentTypes() {
        expect(7) {
            val numbers = listOf("a", "ab", "abc")
            numbers.fold(1) { a, b -> a + b.length }
        }

        expect("1234") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.fold("") { a, b -> a + b }
        }
    }

    @Test fun foldWithNonCommutativeOperation() {
        expect(1) {
            val numbers = listOf(1, 2, 3)
            numbers.fold(7) { a, b -> a - b }
        }
    }

    @Test fun foldRight() {
        expect("1234") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.map { it.toString() }.foldRight("") { a, b -> a + b }
        }
    }

    @Test fun foldRightWithDifferentTypes() {
        expect("1234") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.foldRight("") { a, b -> "" + a + b }
        }
    }

    @Test fun foldRightWithNonCommutativeOperation() {
        expect(-5) {
            val numbers = listOf(1, 2, 3)
            numbers.foldRight(7) { a, b -> a - b }
        }
    }

    @Test
    fun zipTransform() {
        expect(listOf("ab", "bc", "cd")) {
            listOf("a", "b", "c").zip(listOf("b", "c", "d")) { a, b -> a + b }
        }
    }

    @Test
    fun zip() {
        expect(listOf("a" to "b", "b" to "c", "c" to "d")) {
            listOf("a", "b", "c").zip(listOf("b", "c", "d"))
        }
    }

    @Test fun partition() {
        val data = listOf("foo", "bar", "something", "xyz")
        val pair = data.partition { it.length == 3 }

        assertEquals(listOf("foo", "bar", "xyz"), pair.first, "pair.first")
        assertEquals(listOf("something"), pair.second, "pair.second")
    }

    @Test fun reduceIndexed() {
        expect("123") {
            val list = listOf("1", "2", "3", "4")
            list.reduceIndexed { index, a, b -> if (index == 3) a else a + b }
        }

        expect(5) {
            listOf(2, 3).reduceIndexed { index, acc: Number, e ->
                assertEquals(1, index)
                assertEquals(2, acc)
                assertEquals(3, e)
                acc.toInt() + e
            }
        }

        assertFailsWith<UnsupportedOperationException> {
            arrayListOf<Int>().reduceIndexed { index, a, b -> index + a + b }
        }
    }

    @Test fun reduceRightIndexed() {
        expect("234") {
            val list = listOf("1", "2", "3", "4")
            list.reduceRightIndexed { index, a, b -> if (index == 0) b else a + b }
        }

        expect(1) {
            listOf(2, 3).reduceRightIndexed { index, e, acc: Number ->
                assertEquals(0, index)
                assertEquals(3, acc)
                assertEquals(2, e)
                acc.toInt() - e
            }
        }

        assertFailsWith<UnsupportedOperationException> {
            arrayListOf<Int>().reduceRightIndexed { index, a, b -> index + a + b }
        }
    }

    @Test fun reduce() {
        expect("1234") {
            val list = listOf("1", "2", "3", "4")
            list.reduce { a, b -> a + b }
        }

        assertFailsWith<UnsupportedOperationException> {
            arrayListOf<Int>().reduce { a, b -> a + b }
        }
    }

    @Test fun reduceOrNull() {
        expect("1234") {
            val list = listOf("1", "2", "3", "4")
            list.reduceOrNull { a, b -> a + b }
        }

        expect(null, { arrayListOf<Int>().reduceOrNull { a, b -> a + b } })
    }

    @Test fun reduceRight() {
        expect("1234") {
            val list = listOf("1", "2", "3", "4")
            list.reduceRight { a, b -> a + b }
        }

        assertFailsWith<UnsupportedOperationException> {
            arrayListOf<Int>().reduceRight { a, b -> a + b }
        }
    }

    @Test fun reduceRightOrNull() {
        expect("1234") {
            val list = listOf("1", "2", "3", "4")
            list.reduceRightOrNull { a, b -> a + b }
        }

        expect(null, { arrayListOf<Int>().reduceRightOrNull { a, b -> a + b } })
    }

    @Test fun groupBy() {
        val words = listOf("a", "abc", "ab", "def", "abcd")
        val byLength = words.groupBy { it.length }
        assertEquals(4, byLength.size)

        // verify that order of keys is preserved
        assertEquals(listOf(
                1 to listOf("a"),
                3 to listOf("abc", "def"),
                2 to listOf("ab"),
                4 to listOf("abcd")
        ), byLength.toList())

        val l3 = byLength[3].orEmpty()
        assertEquals(listOf("abc", "def"), l3)
    }

    @Test fun groupByKeysAndValues() {
        val nameToTeam = listOf("Alice" to "Marketing", "Bob" to "Sales", "Carol" to "Marketing")
        val namesByTeam = nameToTeam.groupBy({ it.second }, { it.first })
        assertEquals(
                listOf(
                    "Marketing" to listOf("Alice", "Carol"),
                    "Sales" to listOf("Bob")
                ),
                namesByTeam.toList())


        val mutableNamesByTeam = nameToTeam.groupByTo(HashMap(), { it.second }, { it.first })
        assertEquals(namesByTeam, mutableNamesByTeam)
    }

    @Test fun associateWith() {
        val items = listOf("Alice", "Bob", "Carol")
        val itemsWithTheirLength = items.associateWith { it.length }

        assertEquals(mapOf("Alice" to 5, "Bob" to 3, "Carol" to 5), itemsWithTheirLength)

        val updatedLength =
            items.drop(1).associateWithTo(itemsWithTheirLength.toMutableMap()) { name -> name.toLowerCase().count { it in "aeuio" }}

        assertEquals(mapOf("Alice" to 5, "Bob" to 1, "Carol" to 2), updatedLength)
    }

    @Test fun plusRanges() {
        val range1 = 1..3
        val range2 = 4..7
        val combined = range1 + range2
        assertEquals((1..7).toList(), combined)
    }

    @Test fun mapRanges() {
        val range = (1..3).map { it * 2 }
        assertEquals(listOf(2, 4, 6), range)
    }

    fun testPlus(doPlus: (List<String>) -> List<String>) {
        val list = listOf("foo", "bar")
        val list2: List<String> = doPlus(list)
        assertEquals(listOf("foo", "bar"), list)
        assertEquals(listOf("foo", "bar", "cheese", "wine"), list2)
    }

    @Test fun plusElement() = testPlus { it + "cheese" + "wine" }
    @Test fun plusCollection() = testPlus { it + listOf("cheese", "wine") }
    @Test fun plusArray() = testPlus { it + arrayOf("cheese", "wine") }
    @Test fun plusSequence() = testPlus { it + sequenceOf("cheese", "wine") }

    @Test fun plusCollectionBug() {
        val list = listOf("foo", "bar") + listOf("cheese", "wine")
        assertEquals(listOf("foo", "bar", "cheese", "wine"), list)
    }

    @Test fun plusCollectionInference() {
        val listOfLists = listOf(listOf("s"))
        val elementList = listOf("a")
        val result: List<List<String>> = listOfLists.plusElement(elementList)
        assertEquals(listOf(listOf("s"), listOf("a")), result, "should be list + element")

        val listOfAny = listOf<Any>("a") + listOf<Any>("b")
        assertEquals(listOf("a", "b"), listOfAny,  "should be list + list")

        val listOfAnyAndList = listOf<Any>("a") + listOf<Any>("b") as Any
        assertEquals(listOf("a", listOf("b")), listOfAnyAndList, "should be list + Any")
    }

    @Test fun plusAssign() {
        // lets use a mutable variable of readonly list
        var l: List<String> = listOf("cheese")
        val lOriginal = l
        l += "foo"
        l += listOf("beer")
        l += arrayOf("cheese", "wine")
        l += sequenceOf("bar", "foo")
        assertEquals(listOf("cheese", "foo", "beer", "cheese", "wine", "bar", "foo"), l)
        assertTrue(l !== lOriginal)

        val ml = arrayListOf("cheese")
        ml += "foo"
        ml += listOf("beer")
        ml += arrayOf("cheese", "wine")
        ml += sequenceOf("bar", "foo")
        assertEquals(l, ml)
    }


    private fun testMinus(expected: List<String>? = null, doMinus: (List<String>) -> List<String>) {
        val a = listOf("foo", "bar", "bar")
        val b: List<String> = doMinus(a)
        val expected_ = expected ?: listOf("foo")
        assertEquals(expected_, b.toList())
    }

    @Test fun minusElement() = testMinus(expected = listOf("foo", "bar")) { it - "bar" - "zoo" }
    @Test fun minusCollection() = testMinus { it - listOf("bar", "zoo") }
    @Test fun minusArray() = testMinus { it - arrayOf("bar", "zoo") }
    @Test fun minusSequence() = testMinus { it - sequenceOf("bar", "zoo") }

    @Test fun minusIsEager() {
        val source = listOf("foo", "bar")
        val list = arrayListOf<String>()
        val result = source - list

        list += "foo"
        assertEquals(source, result)
        list += "bar"
        assertEquals(source, result)
    }

    @Test fun minusAssign() {
        // lets use a mutable variable of readonly list
        val data: List<String> = listOf("cheese", "foo", "beer", "cheese", "wine")
        var l = data
        l -= "cheese"
        assertEquals(listOf("foo", "beer", "cheese", "wine"), l)
        l = data
        l -= listOf("cheese", "beer")
        assertEquals(listOf("foo", "wine"), l)
        l -= arrayOf("wine", "bar")
        assertEquals(listOf("foo"), l)

        val ml = arrayListOf("cheese", "cheese", "foo", "beer", "cheese", "wine")
        ml -= "cheese"
        assertEquals(listOf("cheese", "foo", "beer", "cheese", "wine"), ml)
        ml -= listOf("cheese", "beer")
        assertEquals(listOf("foo", "wine"), ml)
        ml -= arrayOf("wine", "bar")
        assertEquals(listOf("foo"), ml)
    }



    @Test fun requireNoNulls() {
        val data = arrayListOf<String?>("foo", "bar")
        val notNull = data.requireNoNulls()
        assertEquals(listOf("foo", "bar"), notNull)

        val hasNulls = listOf("foo", null, "bar")

        assertFailsWith<IllegalArgumentException> {
            // should throw an exception as we have a null
            hasNulls.requireNoNulls()
        }
    }

    @Test fun reverseInPlace() {
        val data = arrayListOf<String>()
        data.reverse()
        assertTrue(data.isEmpty())

        data.add("foo")
        data.reverse()
        assertEquals(listOf("foo"), data)

        data.add("bar")
        data.reverse()
        assertEquals(listOf("bar", "foo"), data)

        data.add("zoo")
        data.reverse()
        assertEquals(listOf("zoo", "foo", "bar"), data)
    }

    @Test fun reversed() {
        val data = listOf("foo", "bar")
        val rev = data.reversed()
        assertEquals(listOf("bar", "foo"), rev)
        assertNotEquals(data, rev)
    }


    @Test fun drop() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(listOf("bar", "abc"), coll.drop(1))
        assertEquals(listOf("abc"), coll.drop(2))
    }

    @Test fun dropWhile() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(listOf("bar", "abc"), coll.dropWhile { it.startsWith("f") })
    }

    @Test fun dropLast() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(coll, coll.dropLast(0))
        assertEquals(emptyList<String>(), coll.dropLast(coll.size))
        assertEquals(emptyList<String>(), coll.dropLast(coll.size + 1))
        assertEquals(listOf("foo", "bar"), coll.dropLast(1))
        assertEquals(listOf("foo"), coll.dropLast(2))

        assertFails { coll.dropLast(-1) }
    }

    @Test fun dropLastWhile() {
        val coll = listOf("Foo", "bare", "abc" )
        assertEquals(coll, coll.dropLastWhile { false })
        assertEquals(listOf<String>(), coll.dropLastWhile { true })
        assertEquals(listOf("Foo", "bare"), coll.dropLastWhile { it.length < 4 })
        assertEquals(listOf("Foo"), coll.dropLastWhile { it.all { it in 'a'..'z' } })
    }

    @Test fun take() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(emptyList<String>(), coll.take(0))
        assertEquals(listOf("foo"), coll.take(1))
        assertEquals(listOf("foo", "bar"), coll.take(2))
        assertEquals(coll, coll.take(coll.size))
        assertEquals(coll, coll.take(coll.size + 1))

        assertFails { coll.take(-1) }
    }

    @Test fun takeWhile() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(emptyList<String>(), coll.takeWhile { false })
        assertEquals(coll, coll.takeWhile { true })
        assertEquals(listOf("foo"), coll.takeWhile { it.startsWith("f") })
        assertEquals(listOf("foo", "bar", "abc"), coll.takeWhile { it.length == 3 })
    }

    @Test fun takeLast() {
        val coll = listOf("foo", "bar", "abc")

        assertEquals(emptyList<String>(), coll.takeLast(0))
        assertEquals(listOf("abc"), coll.takeLast(1))
        assertEquals(listOf("bar", "abc"), coll.takeLast(2))
        assertEquals(coll, coll.takeLast(coll.size))
        assertEquals(coll, coll.takeLast(coll.size + 1))

        assertFails { coll.takeLast(-1) }

        val collWithoutRandomAccess = object : List<String> by coll {}
        assertEquals(listOf("abc"), collWithoutRandomAccess.takeLast(1))
        assertEquals(listOf("bar", "abc"), collWithoutRandomAccess.takeLast(2))
    }

    @Test fun takeLastWhile() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(emptyList<String>(), coll.takeLastWhile { false })
        assertEquals(coll, coll.takeLastWhile { true })
        assertEquals(listOf("abc"), coll.takeLastWhile { it.startsWith("a") })
        assertEquals(listOf("bar", "abc"), coll.takeLastWhile { it[0] < 'c' })
    }

    @Test fun copyToArray() {
        val data = listOf("foo", "bar")
        val arr = data.toTypedArray()
        println("Got array ${arr}")
        assertEquals(2, arr.size)
    }

    @Test fun count() {
        val data = listOf("foo", "bar")
        assertEquals(2, data.count())
        assertEquals(3, hashSetOf(12, 14, 15).count())
        assertEquals(0, ArrayList<Double>().count())
    }

    @Test fun first() {
        val data = listOf("foo", "bar")
        assertEquals("foo", data.first())
        assertEquals(15, listOf(15, 19, 20, 25).first())
        assertEquals('a', listOf('a').first())
        assertFails { arrayListOf<Int>().first() }
    }

    @Test fun last() {
        val data = listOf("foo", "bar")
        assertEquals("bar", data.last())
        assertEquals(25, listOf(15, 19, 20, 25).last())
        assertEquals('a', listOf('a').last())
        assertFails { arrayListOf<Int>().last() }
    }

    @Test fun random() {
        val list = List(100) { it }
        val set = list.toSet()
        listOf(list, set).forEach { collection: Collection<Int> ->
            val tosses = List(10) { collection.random() }
            assertTrue(tosses.distinct().size > 1, "Should be some distinct elements in $tosses")

            val seed = Random.nextInt()
            val random1 = Random(seed)
            val random2 = Random(seed)

            val tosses1 = List(10) { collection.random(random1) }
            val tosses2 = List(10) { collection.random(random2) }

            assertEquals(tosses1, tosses2)
        }

        listOf("x").let { singletonList ->
            val tosses = List(10) { singletonList.random() }
            assertEquals(singletonList, tosses.distinct())
        }

        assertFailsWith<NoSuchElementException> { emptyList<Any>().random() }
    }

    @Test fun randomOrNull() {
        val list = List(100) { it }
        val set = list.toSet()
        listOf(list, set).forEach { collection: Collection<Int> ->
            val tosses = List(10) { collection.randomOrNull() }
            assertTrue(tosses.distinct().size > 1, "Should be some distinct elements in $tosses")

            val seed = Random.nextInt()
            val random1 = Random(seed)
            val random2 = Random(seed)

            val tosses1 = List(10) { collection.randomOrNull(random1) }
            val tosses2 = List(10) { collection.randomOrNull(random2) }

            assertEquals(tosses1, tosses2)
        }

        listOf("x").let { singletonList ->
            val tosses = List(10) { singletonList.randomOrNull() }
            assertEquals(singletonList, tosses.distinct())
        }

        assertNull(emptyList<Any>().randomOrNull())
    }

    @Test fun subscript() {
        val list = arrayListOf("foo", "bar")
        assertEquals("foo", list[0])
        assertEquals("bar", list[1])

        // lists throw an exception if out of range
        assertFails {
            @Suppress("UNUSED_VARIABLE")
            val outOfBounds = list[2]
        }

        // lets try update the list
        list[0] = "new"
        list[1] = "thing"

        // lists don't allow you to set past the end of the list
        assertFails {
            list[2] = "works"
        }

        list.add("works")
        assertEquals(listOf("new", "thing", "works"), list)
    }

    @Test fun indices() {
        val data = listOf("foo", "bar")
        val indices = data.indices
        assertEquals(0, indices.start)
        assertEquals(1, indices.endInclusive)
        assertEquals(0..data.size - 1, indices)
    }

    @Test fun contains() {
        assertFalse(hashSetOf<Int>().contains(12))
        assertTrue(listOf(15, 19, 20).contains(15))

        assertTrue(hashSetOf(45, 14, 13).toIterable().contains(14))
    }

    @Test fun min() {
        expect(null, { listOf<Int>().min() })
        expect(1, { listOf(1).min() })
        expect(2, { listOf(2, 3).min() })
        expect(2000000000000, { listOf(3000000000000, 2000000000000).min() })
        expect('a', { listOf('a', 'b').min() })
        expect("a", { listOf("a", "b").min() })
        expect(null, { listOf<Int>().asSequence().min() })
        expect(2, { listOf(2, 3).asSequence().min() })
    }

    @Test fun max() {
        expect(null, { listOf<Int>().max() })
        expect(1, { listOf(1).max() })
        expect(3, { listOf(2, 3).max() })
        expect(3000000000000, { listOf(3000000000000, 2000000000000).max() })
        expect('b', { listOf('a', 'b').max() })
        expect("b", { listOf("a", "b").max() })
        expect(null, { listOf<Int>().asSequence().max() })
        expect(3, { listOf(2, 3).asSequence().max() })
    }

    @Test fun minWith() {
        expect(null, { listOf<Int>().minWith(naturalOrder()) })
        expect(1, { listOf(1).minWith(naturalOrder()) })
        expect("a", { listOf("a", "B").minWith(STRING_CASE_INSENSITIVE_ORDER) })
        expect("a", { listOf("a", "B").asSequence().minWith(STRING_CASE_INSENSITIVE_ORDER) })
    }

    @Test fun maxWith() {
        expect(null, { listOf<Int>().maxWith(naturalOrder()) })
        expect(1, { listOf(1).maxWith(naturalOrder()) })
        expect("B", { listOf("a", "B").maxWith(STRING_CASE_INSENSITIVE_ORDER) })
        expect("B", { listOf("a", "B").asSequence().maxWith(STRING_CASE_INSENSITIVE_ORDER) })
    }

    @Test fun minBy() {
        expect(null, { listOf<Int>().minBy { it } })
        expect(1, { listOf(1).minBy { it } })
        expect(3, { listOf(2, 3).minBy { -it } })
        expect('a', { listOf('a', 'b').minBy { "x$it" } })
        expect("b", { listOf("b", "abc").minBy { it.length } })
        expect(null, { listOf<Int>().asSequence().minBy { it } })
        expect(3, { listOf(2, 3).asSequence().minBy { -it } })
    }

    @Test fun maxBy() {
        expect(null, { listOf<Int>().maxBy { it } })
        expect(1, { listOf(1).maxBy { it } })
        expect(2, { listOf(2, 3).maxBy { -it } })
        expect('b', { listOf('a', 'b').maxBy { "x$it" } })
        expect("abc", { listOf("b", "abc").maxBy { it.length } })
        expect(null, { listOf<Int>().asSequence().maxBy { it } })
        expect(2, { listOf(2, 3).asSequence().maxBy { -it } })
    }

    @Test fun minByEvaluateOnce() {
        var c = 0
        expect(1, { listOf(5, 4, 3, 2, 1).minBy { c++; it * it } })
        assertEquals(5, c)
        c = 0
        expect(1, { listOf(5, 4, 3, 2, 1).asSequence().minBy { c++; it * it } })
        assertEquals(5, c)
    }

    @Test fun maxByEvaluateOnce() {
        var c = 0
        expect(5, { listOf(5, 4, 3, 2, 1).maxBy { c++; it * it } })
        assertEquals(5, c)
        c = 0
        expect(5, { listOf(5, 4, 3, 2, 1).asSequence().maxBy { c++; it * it } })
        assertEquals(5, c)
    }

    @Test fun sum() {
        expect(0) { arrayListOf<Int>().sum() }
        expect(14) { listOf(2, 3, 9).sum() }
        expect(3.0) { listOf(1.0, 2.0).sum() }
        expect(3000000000000) { arrayListOf<Long>(1000000000000, 2000000000000).sum() }
        expect(3.0.toFloat()) { arrayListOf<Float>(1.0.toFloat(), 2.0.toFloat()).sum() }
        expect(3.0.toFloat()) { sequenceOf<Float>(1.0.toFloat(), 2.0.toFloat()).sum() }
    }

    @Test fun average() {
        assertTrue { arrayListOf<Int>().average().isNaN() }
        expect(3.8) { listOf(1, 2, 5, 8, 3).average() }
        expect(2.1) { sequenceOf(1.6, 2.6, 3.6, 0.6).average() }
        expect(100.0) { arrayListOf<Byte>(100, 100, 100, 100, 100, 100).average() }
        val n = 100
        val range = 0..n
        expect(n.toDouble()/2) { range.average() }
    }

    @Test fun takeReturnsFirstNElements() {
        expect(listOf(1, 2, 3, 4, 5)) { (1..10).take(5) }
        expect(listOf(1, 2, 3, 4, 5)) { (1..10).toList().take(5) }
        expect(listOf(1, 2)) { (1..10).take(2) }
        expect(listOf(1, 2)) { (1..10).toList().take(2) }
        expect(true) { (0L..5L).take(0).none() }
        expect(true) { listOf(1L).take(0).none() }
        expect(listOf(1)) { (1..1).take(10) }
        expect(listOf(1)) { listOf(1).take(10) }
    }

    @Test fun sortInPlace() {
        val data = listOf(11, 3, 7)

        val asc = data.toMutableList()
        asc.sort()
        assertEquals(listOf(3, 7, 11), asc)

        val desc = data.toMutableList()
        desc.sortDescending()
        assertEquals(listOf(11, 7, 3), desc)
    }

    @Test fun sorted() {
        val data = listOf(11, 3, 7)
        assertEquals(listOf(3, 7, 11), data.sorted())
        assertEquals(listOf(11, 7, 3), data.sortedDescending())

        assertEquals(listOf(-0.0, 0.0), listOf(0.0, -0.0).sorted())
        assertNotEquals(listOf(0.0, -0.0), listOf(0.0, -0.0).sorted())

        val dataDouble = listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MIN_VALUE, -Double.MIN_VALUE,
                                1.0, -1.0, Double.MAX_VALUE, -Double.MAX_VALUE, Double.NaN, 0.0, -0.0)
        assertEquals(listOf(Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -1.0, -Double.MIN_VALUE, -0.0,
                            0.0, Double.MIN_VALUE, 1.0, Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NaN), dataDouble.sorted())
        assertEquals(listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE, 1.0, Double.MIN_VALUE, 0.0,
                            -0.0, -Double.MIN_VALUE, -1.0, -Double.MAX_VALUE, Double.NEGATIVE_INFINITY), dataDouble.sortedDescending())
    }

    @Test fun sortByInPlace() {
        val data = arrayListOf("aa" to 20, "ab" to 3, "aa" to 3)
        data.sortBy { it.second }
        assertEquals(listOf("ab" to 3, "aa" to 3, "aa" to 20), data)

        data.sortBy { it.first }
        assertEquals(listOf("aa" to 3, "aa" to 20, "ab" to 3), data)

        data.sortByDescending { (it.first + it.second).length }
        assertEquals(listOf("aa" to 20, "aa" to 3, "ab" to 3), data)
    }

    @Test fun sortStable() {
        val keyRange = 'A'..'D'
        for (size in listOf(10, 100, 2000)) {
            val list = MutableList(size) { index -> Sortable(keyRange.random(), index) }

            list.sorted().assertStableSorted()
            list.sortedDescending().assertStableSorted(descending = true)

            list.sort()
            list.assertStableSorted()
            list.sortDescending()
            list.assertStableSorted(descending = true)
        }
    }

    @Test fun sortedBy() {
        assertEquals(listOf("two" to 3, "three" to 20), listOf("three" to 20, "two" to 3).sortedBy { it.second })
        assertEquals(listOf("three" to 20, "two" to 3), listOf("three" to 20, "two" to 3).sortedBy { it.first })
        assertEquals(listOf("three", "two"), listOf("two", "three").sortedByDescending { it.length })
    }

    @Test fun sortedNullableBy() {
        fun String.nullIfEmpty() = if (isEmpty()) null else this
        listOf(null, "", "a").let {
            expect(listOf(null, "", "a")) { it.sortedWith(nullsFirst(compareBy { it })) }
            expect(listOf("a", "", null)) { it.sortedWith(nullsLast(compareByDescending { it })) }
            expect(listOf(null, "a", "")) { it.sortedWith(nullsFirst(compareByDescending { it.nullIfEmpty() })) }
        }
    }

    @Test fun sortedByNullable() {
        fun String.nonEmptyLength() = if (isEmpty()) null else length
        listOf("", "sort", "abc").let {
            assertEquals(listOf("", "abc", "sort"), it.sortedBy { it.nonEmptyLength() })
            assertEquals(listOf("sort", "abc", ""), it.sortedByDescending { it.nonEmptyLength() })
            assertEquals(listOf("abc", "sort", ""), it.sortedWith(compareBy(nullsLast<Int>()) { it.nonEmptyLength()}))
        }
    }

    @Test fun sortedWith() {
        val comparator = compareBy<String> { it.toUpperCase().reversed() }
        val data = listOf("cat", "dad", "BAD")

        expect(listOf("BAD", "dad", "cat")) { data.sortedWith(comparator) }
        expect(listOf("cat", "dad", "BAD")) { data.sortedWith(comparator.reversed()) }
        expect(listOf("BAD", "dad", "cat")) { data.sortedWith(comparator.reversed().reversed()) }
    }

    @Test fun sortByStable() {
        val keyRange = 'A'..'D'
        for (size in listOf(10, 100, 2000)) {
            val list = MutableList(size) { index -> Sortable(keyRange.random(), index) }

            list.sortedBy { it.key }.assertStableSorted()
            list.sortedByDescending { it.key }.assertStableSorted(descending = true)

            list.sortBy { it.key }
            list.assertStableSorted()

            list.sortByDescending { it.key }
            list.assertStableSorted(descending = true)
        }
    }

    @Test fun decomposeFirst() {
        val (first) = listOf(1, 2)
        assertEquals(first, 1)
    }

    @Test fun decomposeSplit() {
        val (key, value) = "key = value".split("=").map { it.trim() }
        assertEquals(key, "key")
        assertEquals(value, "value")
    }

    @Test fun decomposeList() {
        val (a, b, c, d, e) = listOf(1, 2, 3, 4, 5)
        assertEquals(a, 1)
        assertEquals(b, 2)
        assertEquals(c, 3)
        assertEquals(d, 4)
        assertEquals(e, 5)
    }

    @Test fun decomposeArray() {
        val (a, b, c, d, e) = arrayOf(1, 2, 3, 4, 5)
        assertEquals(a, 1)
        assertEquals(b, 2)
        assertEquals(c, 3)
        assertEquals(d, 4)
        assertEquals(e, 5)
    }

    @Test fun decomposeIntArray() {
        val (a, b, c, d, e) = intArrayOf(1, 2, 3, 4, 5)
        assertEquals(a, 1)
        assertEquals(b, 2)
        assertEquals(c, 3)
        assertEquals(d, 4)
        assertEquals(e, 5)
    }

    @Test fun unzipList() {
        val list = listOf(1 to 'a', 2 to 'b', 3 to 'c')
        val (ints, chars) = list.unzip()
        assertEquals(listOf(1, 2, 3), ints)
        assertEquals(listOf('a', 'b', 'c'), chars)
    }

    @Test fun unzipArray() {
        val array = arrayOf(1 to 'a', 2 to 'b', 3 to 'c')
        val (ints, chars) = array.unzip()
        assertEquals(listOf(1, 2, 3), ints)
        assertEquals(listOf('a', 'b', 'c'), chars)
    }

    @Test fun specialLists() {
        compare(arrayListOf<Int>(), listOf<Int>()) { listBehavior() }
        compare(arrayListOf<Double>(), emptyList<Double>()) { listBehavior() }
        compare(arrayListOf("value"), listOf("value")) { listBehavior() }
    }

    @Test fun specialSets() {
        compare(linkedSetOf<Int>(), setOf<Int>()) { setBehavior() }
        compare(hashSetOf<Double>(), emptySet<Double>()) { setBehavior() }
        compare(listOf("value").toMutableSet(), setOf("value")) { setBehavior() }
    }

    @Test fun specialMaps() {
        compare(hashMapOf<String, Int>(), mapOf<String, Int>()) { mapBehavior() }
        compare(linkedMapOf<Int, String>(), emptyMap<Int, String>()) { mapBehavior() }
        compare(linkedMapOf(2 to 3), mapOf(2 to 3)) { mapBehavior() }
    }

    @Test fun toStringTest() {
        // we need toString() inside pattern because of KT-8666
        assertEquals("[1, a, null, ${Long.MAX_VALUE.toString()}]", listOf(1, "a", null, Long.MAX_VALUE).toString())
    }

    @Test fun randomAccess() {
        assertStaticAndRuntimeTypeIs<RandomAccess>(arrayListOf(1))
        assertTrue(listOf(1, 2) is RandomAccess, "Default read-only list implementation is RandomAccess")
        assertTrue(listOf(1) is RandomAccess, "Default singleton list is RandomAccess")
        assertTrue(emptyList<Int>() is RandomAccess, "Empty list is RandomAccess")
    }

    @Test fun abstractCollectionToArray() {
        class TestCollection<out E>(val data: Collection<E>) : AbstractCollection<E>() {
            val invocations = mutableListOf<String>()
            override val size get() = data.size
            override fun iterator() = data.iterator()

            override fun toArray(): Array<Any?> {
                invocations += "toArray1"
                return data.toTypedArray()
            }
            public override fun <T> toArray(array: Array<T>): Array<T> {
                invocations += "toArray2"
                return super.toArray(array)
            }
        }
        val data = listOf("abc", "def")
        val coll = TestCollection(data)

        val arr1 = coll.toTypedArray()
        assertEquals(data, arr1.asList())
        assertTrue("toArray1" in coll.invocations || "toArray2" in coll.invocations)

        val arr2: Array<String> = coll.toArray(Array(coll.size + 1) { "" })
        assertEquals(data + listOf(null), arr2.asList())
    }
}
