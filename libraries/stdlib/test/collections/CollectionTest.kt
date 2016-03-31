/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package test.collections

import java.util.*
import kotlin.test.*
import org.junit.Test as test
import test.collections.behaviors.*
import test.compare.STRING_CASE_INSENSITIVE_ORDER
import java.io.Serializable
import kotlin.comparisons.*

class CollectionTest {

    @test fun joinTo() {
        val data = listOf("foo", "bar")
        val buffer = StringBuilder()
        data.joinTo(buffer, "-", "{", "}")
        assertEquals("{foo-bar}", buffer.toString())
    }

    @test fun joinToString() {
        val data = listOf("foo", "bar")
        val text = data.joinToString("-", "<", ">")
        assertEquals("<foo-bar>", text)

        val big = listOf("a", "b", "c", "d", "e", "f")
        val text2 = big.joinToString(limit = 3, truncated = "*")
        assertEquals("a, b, c, *", text2)
    }

    @test fun filterNotNull() {
        val data = listOf(null, "foo", null, "bar")
        val foo = data.filterNotNull()

        assertEquals(2, foo.size)
        assertEquals(listOf("foo", "bar"), foo)

        assertTrue {
            foo is List<String>
        }
    }

    /*
    @test fun mapNotNull() {
        val data = listOf(null, "foo", null, "bar")
        val foo = data.mapNotNull { it.length() }
        assertEquals(2, foo.size())
        assertEquals(listOf(3, 3), foo)

        assertTrue {
            foo is List<Int>
        }
    }
    */

    @test fun listOfNotNull() {
        val l1: List<Int> = listOfNotNull(null)
        assertTrue(l1.isEmpty())

        val s: String? = "value"
        val l2: List<String> = listOfNotNull(s)
        assertEquals(s, l2.single())

        val l3: List<String> = listOfNotNull("value1", null, "value2")
        assertEquals(listOf("value1", "value2"), l3)
    }

    @test fun filterIntoSet() {
        val data = listOf("foo", "bar")
        val foo = data.filterTo(hashSetOf<String>()) { it.startsWith("f") }

        assertTrue {
            foo.all { it.startsWith("f") }
        }
        assertEquals(1, foo.size)
        assertEquals(hashSetOf("foo"), foo)

        assertTrue {
            foo is HashSet<String>
        }
    }

    @test fun foldIndexed() {
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

    @test fun foldIndexedWithDifferentTypes() {
        expect(10) {
            val numbers = listOf("a", "ab", "abc")
            numbers.foldIndexed(1) { index, a, b -> a + b.length + index }
        }

        expect("11223344") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.foldIndexed("") { index, a, b -> a + b + (index + 1) }
        }
    }

    @test fun foldIndexedWithNonCommutativeOperation() {
        expect(4) {
            val numbers = listOf(1, 2, 3)
            numbers.foldIndexed(7) { index, a, b -> index + a - b }
        }
    }

    @test fun foldRightIndexed() {
        expect("12343210") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.map { it.toString() }.foldRightIndexed("") { index, a, b -> a + b + index }
        }
    }

    @test fun foldRightIndexedWithDifferentTypes() {
        expect("12343210") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.foldRightIndexed("") { index, a, b -> "" + a + b + index }
        }
    }

    @test fun foldRightIndexedWithNonCommutativeOperation() {
        expect(-4) {
            val numbers = listOf(1, 2, 3)
            numbers.foldRightIndexed(7) { index, a, b -> index + a - b }
        }
    }

    @test fun fold() {
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

    @test fun foldWithDifferentTypes() {
        expect(7) {
            val numbers = listOf("a", "ab", "abc")
            numbers.fold(1) { a, b -> a + b.length }
        }

        expect("1234") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.fold("") { a, b -> a + b }
        }
    }

    @test fun foldWithNonCommutativeOperation() {
        expect(1) {
            val numbers = listOf(1, 2, 3)
            numbers.fold(7) { a, b -> a - b }
        }
    }

    @test fun foldRight() {
        expect("1234") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.map { it.toString() }.foldRight("") { a, b -> a + b }
        }
    }

    @test fun foldRightWithDifferentTypes() {
        expect("1234") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.foldRight("") { a, b -> "" + a + b }
        }
    }

    @test fun foldRightWithNonCommutativeOperation() {
        expect(-5) {
            val numbers = listOf(1, 2, 3)
            numbers.foldRight(7) { a, b -> a - b }
        }
    }

    @test
    fun zipTransform() {
        expect(listOf("ab", "bc", "cd")) {
            listOf("a", "b", "c").zip(listOf("b", "c", "d")) { a, b -> a + b }
        }
    }

    @test
    fun zip() {
        expect(listOf("a" to "b", "b" to "c", "c" to "d")) {
            listOf("a", "b", "c").zip(listOf("b", "c", "d"))
        }
    }

    @test fun partition() {
        val data = listOf("foo", "bar", "something", "xyz")
        val pair = data.partition { it.length == 3 }

        assertEquals(listOf("foo", "bar", "xyz"), pair.first, "pair.first")
        assertEquals(listOf("something"), pair.second, "pair.second")
    }

    @test fun reduceIndexed() {
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

        assertTrue(assertFails {
            arrayListOf<Int>().reduceIndexed { index, a, b -> a + b }
        } is UnsupportedOperationException)
    }

    @test fun reduceRightIndexed() {
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

        assertTrue(assertFails {
            arrayListOf<Int>().reduceRightIndexed { index, a, b -> a + b }
        } is UnsupportedOperationException)
    }

    @test fun reduce() {
        expect("1234") {
            val list = listOf("1", "2", "3", "4")
            list.reduce { a, b -> a + b }
        }

        assertTrue(assertFails {
            arrayListOf<Int>().reduce { a, b -> a + b }
        } is UnsupportedOperationException)
    }

    @test fun reduceRight() {
        expect("1234") {
            val list = listOf("1", "2", "3", "4")
            list.reduceRight { a, b -> a + b }
        }

        assertTrue(assertFails {
            arrayListOf<Int>().reduceRight { a, b -> a + b }
        } is UnsupportedOperationException)
    }

    @test fun groupBy() {
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

    @test fun groupByKeysAndValues() {
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

    @test fun plusRanges() {
        val range1 = 1..3
        val range2 = 4..7
        val combined = range1 + range2
        assertEquals((1..7).toList(), combined)
    }

    @test fun mapRanges() {
        val range = (1..3).map { it * 2 }
        assertEquals(listOf(2, 4, 6), range)
    }

    fun testPlus(doPlus: (List<String>) -> List<String>) {
        val list = listOf("foo", "bar")
        val list2: List<String> = doPlus(list)
        assertEquals(listOf("foo", "bar"), list)
        assertEquals(listOf("foo", "bar", "cheese", "wine"), list2)
    }

    @test fun plusElement() = testPlus { it + "cheese" + "wine" }
    @test fun plusCollection() = testPlus { it + listOf("cheese", "wine") }
    @test fun plusArray() = testPlus { it + arrayOf("cheese", "wine") }
    @test fun plusSequence() = testPlus { it + sequenceOf("cheese", "wine") }

    @test fun plusCollectionBug() {
        val list = listOf("foo", "bar") + listOf("cheese", "wine")
        assertEquals(listOf("foo", "bar", "cheese", "wine"), list)
    }

    @test fun plusCollectionInference() {
        val listOfLists = listOf(listOf("s"))
        val elementList = listOf("a")
        val result: List<List<String>> = listOfLists.plusElement(elementList)
        assertEquals(listOf(listOf("s"), listOf("a")), result, "should be list + element")

        val listOfAny = listOf<Any>("a") + listOf<Any>("b")
        assertEquals(listOf("a", "b"), listOfAny,  "should be list + list")

        val listOfAnyAndList = listOf<Any>("a") + listOf<Any>("b") as Any
        assertEquals(listOf("a", listOf("b")), listOfAnyAndList, "should be list + Any")
    }

    @test fun plusAssign() {
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

    @test fun minusElement() = testMinus(expected = listOf("foo", "bar")) { it - "bar" - "zoo" }
    @test fun minusCollection() = testMinus { it - listOf("bar", "zoo") }
    @test fun minusArray() = testMinus { it - arrayOf("bar", "zoo") }
    @test fun minusSequence() = testMinus { it - sequenceOf("bar", "zoo") }

    @test fun minusIsEager() {
        val source = listOf("foo", "bar")
        val list = arrayListOf<String>()
        val result = source - list

        list += "foo"
        assertEquals(source, result)
        list += "bar"
        assertEquals(source, result)
    }

    @test fun minusAssign() {
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



    @test fun requireNoNulls() {
        val data = arrayListOf<String?>("foo", "bar")
        val notNull = data.requireNoNulls()
        assertEquals(listOf("foo", "bar"), notNull)

        val hasNulls = listOf("foo", null, "bar")

        //        TODO replace with more accurate version when KT-5987 will be fixed
        //        failsWith(javaClass<IllegalArgumentException>()) {
        assertFails {
            // should throw an exception as we have a null
            hasNulls.requireNoNulls()
        }
    }

    @test fun reverseInPlace() {
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

    @test fun reversed() {
        val data = listOf("foo", "bar")
        val rev = data.reversed()
        assertEquals(listOf("bar", "foo"), rev)
        assertNotEquals(data, rev)
    }


    @test fun drop() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(listOf("bar", "abc"), coll.drop(1))
        assertEquals(listOf("abc"), coll.drop(2))
    }

    @test fun dropWhile() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(listOf("bar", "abc"), coll.dropWhile { it.startsWith("f") })
    }

    @test fun dropLast() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(coll, coll.dropLast(0))
        assertEquals(emptyList<String>(), coll.dropLast(coll.size))
        assertEquals(emptyList<String>(), coll.dropLast(coll.size + 1))
        assertEquals(listOf("foo", "bar"), coll.dropLast(1))
        assertEquals(listOf("foo"), coll.dropLast(2))

        assertFails { coll.dropLast(-1) }
    }

    @test fun dropLastWhile() {
        val coll = listOf("Foo", "bare", "abc" )
        assertEquals(coll, coll.dropLastWhile { false })
        assertEquals(listOf<String>(), coll.dropLastWhile { true })
        assertEquals(listOf("Foo", "bare"), coll.dropLastWhile { it.length < 4 })
        assertEquals(listOf("Foo"), coll.dropLastWhile { it.all { it in 'a'..'z' } })
    }

    @test fun take() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(emptyList<String>(), coll.take(0))
        assertEquals(listOf("foo"), coll.take(1))
        assertEquals(listOf("foo", "bar"), coll.take(2))
        assertEquals(coll, coll.take(coll.size))
        assertEquals(coll, coll.take(coll.size + 1))

        assertFails { coll.take(-1) }
    }

    @test fun takeWhile() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(emptyList<String>(), coll.takeWhile { false })
        assertEquals(coll, coll.takeWhile { true })
        assertEquals(listOf("foo"), coll.takeWhile { it.startsWith("f") })
        assertEquals(listOf("foo", "bar", "abc"), coll.takeWhile { it.length == 3 })
    }

    @test fun takeLast() {
        val coll = listOf("foo", "bar", "abc")

        assertEquals(emptyList<String>(), coll.takeLast(0))
        assertEquals(listOf("abc"), coll.takeLast(1))
        assertEquals(listOf("bar", "abc"), coll.takeLast(2))
        assertEquals(coll, coll.takeLast(coll.size))
        assertEquals(coll, coll.takeLast(coll.size + 1))

        assertFails { coll.takeLast(-1) }
    }

    @test fun takeLastWhile() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(emptyList<String>(), coll.takeLastWhile { false })
        assertEquals(coll, coll.takeLastWhile { true })
        assertEquals(listOf("abc"), coll.takeLastWhile { it.startsWith("a") })
        assertEquals(listOf("bar", "abc"), coll.takeLastWhile { it[0] < 'c' })
    }

    @test fun copyToArray() {
        val data = listOf("foo", "bar")
        val arr = data.toTypedArray()
        println("Got array ${arr}")
        assertEquals(2, arr.size)
    }

    @test fun count() {
        val data = listOf("foo", "bar")
        assertEquals(2, data.count())
        assertEquals(3, hashSetOf(12, 14, 15).count())
        assertEquals(0, ArrayList<Double>().count())
    }

    @test fun first() {
        val data = listOf("foo", "bar")
        assertEquals("foo", data.first())
        assertEquals(15, listOf(15, 19, 20, 25).first())
        assertEquals('a', listOf('a').first())
        assertFails { arrayListOf<Int>().first() }
    }

    @test fun last() {
        val data = listOf("foo", "bar")
        assertEquals("bar", data.last())
        assertEquals(25, listOf(15, 19, 20, 25).last())
        assertEquals('a', listOf('a').last())
        assertFails { arrayListOf<Int>().last() }
    }

    @test fun subscript() {
        val list = arrayListOf("foo", "bar")
        assertEquals("foo", list[0])
        assertEquals("bar", list[1])

        // lists throw an exception if out of range
        assertFails {
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

    @test fun indices() {
        val data = listOf("foo", "bar")
        val indices = data.indices
        assertEquals(0, indices.start)
        assertEquals(1, indices.endInclusive)
        assertEquals(0..data.size - 1, indices)
    }

    @test fun contains() {
        assertFalse(hashSetOf<Int>().contains(12))
        assertTrue(listOf(15, 19, 20).contains(15))

        assertTrue(hashSetOf(45, 14, 13).toIterable().contains(14))
    }

    @test fun min() {
        expect(null, { listOf<Int>().min() })
        expect(1, { listOf(1).min() })
        expect(2, { listOf(2, 3).min() })
        expect(2000000000000, { listOf(3000000000000, 2000000000000).min() })
        expect('a', { listOf('a', 'b').min() })
        expect("a", { listOf("a", "b").min() })
        expect(null, { listOf<Int>().asSequence().min() })
        expect(2, { listOf(2, 3).asSequence().min() })
    }

    @test fun max() {
        expect(null, { listOf<Int>().max() })
        expect(1, { listOf(1).max() })
        expect(3, { listOf(2, 3).max() })
        expect(3000000000000, { listOf(3000000000000, 2000000000000).max() })
        expect('b', { listOf('a', 'b').max() })
        expect("b", { listOf("a", "b").max() })
        expect(null, { listOf<Int>().asSequence().max() })
        expect(3, { listOf(2, 3).asSequence().max() })
    }

    @test fun minWith() {
        expect(null, { listOf<Int>().minWith(naturalOrder()) })
        expect(1, { listOf(1).minWith(naturalOrder()) })
        expect("a", { listOf("a", "B").minWith(STRING_CASE_INSENSITIVE_ORDER) })
        expect("a", { listOf("a", "B").asSequence().minWith(STRING_CASE_INSENSITIVE_ORDER) })
    }

    @test fun maxWith() {
        expect(null, { listOf<Int>().maxWith(naturalOrder()) })
        expect(1, { listOf(1).maxWith(naturalOrder()) })
        expect("B", { listOf("a", "B").maxWith(STRING_CASE_INSENSITIVE_ORDER) })
        expect("B", { listOf("a", "B").asSequence().maxWith(STRING_CASE_INSENSITIVE_ORDER) })
    }

    @test fun minBy() {
        expect(null, { listOf<Int>().minBy { it } })
        expect(1, { listOf(1).minBy { it } })
        expect(3, { listOf(2, 3).minBy { -it } })
        expect('a', { listOf('a', 'b').minBy { "x$it" } })
        expect("b", { listOf("b", "abc").minBy { it.length } })
        expect(null, { listOf<Int>().asSequence().minBy { it } })
        expect(3, { listOf(2, 3).asSequence().minBy { -it } })
    }

    @test fun maxBy() {
        expect(null, { listOf<Int>().maxBy { it } })
        expect(1, { listOf(1).maxBy { it } })
        expect(2, { listOf(2, 3).maxBy { -it } })
        expect('b', { listOf('a', 'b').maxBy { "x$it" } })
        expect("abc", { listOf("b", "abc").maxBy { it.length } })
        expect(null, { listOf<Int>().asSequence().maxBy { it } })
        expect(2, { listOf(2, 3).asSequence().maxBy { -it } })
    }

    @test fun minByEvaluateOnce() {
        var c = 0
        expect(1, { listOf(5, 4, 3, 2, 1).minBy { c++; it * it } })
        assertEquals(5, c)
        c = 0
        expect(1, { listOf(5, 4, 3, 2, 1).asSequence().minBy { c++; it * it } })
        assertEquals(5, c)
    }

    @test fun maxByEvaluateOnce() {
        var c = 0
        expect(5, { listOf(5, 4, 3, 2, 1).maxBy { c++; it * it } })
        assertEquals(5, c)
        c = 0
        expect(5, { listOf(5, 4, 3, 2, 1).asSequence().maxBy { c++; it * it } })
        assertEquals(5, c)
    }

    @test fun sum() {
        expect(0) { arrayListOf<Int>().sum() }
        expect(14) { listOf(2, 3, 9).sum() }
        expect(3.0) { listOf(1.0, 2.0).sum() }
        expect(3000000000000) { arrayListOf<Long>(1000000000000, 2000000000000).sum() }
        expect(3.0.toFloat()) { arrayListOf<Float>(1.0.toFloat(), 2.0.toFloat()).sum() }
        expect(3.0.toFloat()) { sequenceOf<Float>(1.0.toFloat(), 2.0.toFloat()).sum() }
    }

    @test fun average() {
        expect(0.0) { arrayListOf<Int>().average() }
        expect(3.8) { listOf(1, 2, 5, 8, 3).average() }
        expect(2.1) { sequenceOf(1.6, 2.6, 3.6, 0.6).average() }
        expect(100.0) { arrayListOf<Byte>(100, 100, 100, 100, 100, 100).average() }
        val n = 100
        val range = 0..n
        expect(n.toDouble()/2) { range.average() }
    }

    @test fun takeReturnsFirstNElements() {
        expect(listOf(1, 2, 3, 4, 5)) { (1..10).take(5) }
        expect(listOf(1, 2, 3, 4, 5)) { (1..10).toList().take(5) }
        expect(listOf(1, 2)) { (1..10).take(2) }
        expect(listOf(1, 2)) { (1..10).toList().take(2) }
        expect(true) { (0L..5L).take(0).none() }
        expect(true) { listOf(1L).take(0).none() }
        expect(listOf(1)) { (1..1).take(10) }
        expect(listOf(1)) { listOf(1).take(10) }
    }

    @test fun sortInPlace() {
        val data = listOf(11, 3, 7)

        val asc = data.toArrayList()
        asc.sort()
        assertEquals(listOf(3, 7, 11), asc)

        val desc = data.toArrayList()
        desc.sortDescending()
        assertEquals(listOf(11, 7, 3), desc)
    }

    @test fun sorted() {
        val data = listOf(11, 3, 7)
        assertEquals(listOf(3, 7, 11), data.sorted())
        assertEquals(listOf(11, 7, 3), data.sortedDescending())
    }

    @test fun sortByInPlace() {
        val data = arrayListOf("aa" to 20, "ab" to 3, "aa" to 3)
        data.sortBy { it.second }
        assertEquals(listOf("ab" to 3, "aa" to 3, "aa" to 20), data)

        data.sortBy { it.first }
        assertEquals(listOf("aa" to 3, "aa" to 20, "ab" to 3), data)

        data.sortByDescending { (it.first + it.second).length }
        assertEquals(listOf("aa" to 20, "aa" to 3, "ab" to 3), data)
    }

    @test fun sortedBy() {
        assertEquals(listOf("two" to 3, "three" to 20), listOf("three" to 20, "two" to 3).sortedBy { it.second })
        assertEquals(listOf("three" to 20, "two" to 3), listOf("three" to 20, "two" to 3).sortedBy { it.first })
        assertEquals(listOf("three", "two"), listOf("two", "three").sortedByDescending { it.length })
    }

    @test fun sortedNullableBy() {
        fun String.nullIfEmpty() = if (isEmpty()) null else this
        listOf(null, "", "a").let {
            expect(listOf(null, "", "a")) { it.sortedWith(nullsFirst(compareBy { it })) }
            expect(listOf("a", "", null)) { it.sortedWith(nullsLast(compareByDescending { it })) }
            expect(listOf(null, "a", "")) { it.sortedWith(nullsFirst(compareByDescending { it.nullIfEmpty() })) }
        }
    }

    @test fun sortedByNullable() {
        fun String.nonEmptyLength() = if (isEmpty()) null else length
        listOf("", "sort", "abc").let {
            assertEquals(listOf("", "abc", "sort"), it.sortedBy { it.nonEmptyLength() })
            assertEquals(listOf("sort", "abc", ""), it.sortedByDescending { it.nonEmptyLength() })
            assertEquals(listOf("abc", "sort", ""), it.sortedWith(compareBy(nullsLast<Int>()) { it.nonEmptyLength()}))
        }
    }

    @test fun sortedWith() {
        val comparator = compareBy<String> { it.toUpperCase().reversed() }
        val data = listOf("cat", "dad", "BAD")

        expect(listOf("BAD", "dad", "cat")) { data.sortedWith(comparator) }
        expect(listOf("cat", "dad", "BAD")) { data.sortedWith(comparator.reversed()) }
        expect(listOf("BAD", "dad", "cat")) { data.sortedWith(comparator.reversed().reversed()) }
    }

    @test fun decomposeFirst() {
        val (first) = listOf(1, 2)
        assertEquals(first, 1)
    }

    @test fun decomposeSplit() {
        val (key, value) = "key = value".split("=").map { it.trim() }
        assertEquals(key, "key")
        assertEquals(value, "value")
    }

    @test fun decomposeList() {
        val (a, b, c, d, e) = listOf(1, 2, 3, 4, 5)
        assertEquals(a, 1)
        assertEquals(b, 2)
        assertEquals(c, 3)
        assertEquals(d, 4)
        assertEquals(e, 5)
    }

    @test fun decomposeArray() {
        val (a, b, c, d, e) = arrayOf(1, 2, 3, 4, 5)
        assertEquals(a, 1)
        assertEquals(b, 2)
        assertEquals(c, 3)
        assertEquals(d, 4)
        assertEquals(e, 5)
    }

    @test fun decomposeIntArray() {
        val (a, b, c, d, e) = intArrayOf(1, 2, 3, 4, 5)
        assertEquals(a, 1)
        assertEquals(b, 2)
        assertEquals(c, 3)
        assertEquals(d, 4)
        assertEquals(e, 5)
    }

    @test fun unzipList() {
        val list = listOf(1 to 'a', 2 to 'b', 3 to 'c')
        val (ints, chars) = list.unzip()
        assertEquals(listOf(1, 2, 3), ints)
        assertEquals(listOf('a', 'b', 'c'), chars)
    }

    @test fun unzipArray() {
        val array = arrayOf(1 to 'a', 2 to 'b', 3 to 'c')
        val (ints, chars) = array.unzip()
        assertEquals(listOf(1, 2, 3), ints)
        assertEquals(listOf('a', 'b', 'c'), chars)
    }

    @test fun specialLists() {
        compare(arrayListOf<Int>(), listOf<Int>()) { listBehavior() }
        compare(arrayListOf<Double>(), emptyList<Double>()) { listBehavior() }
        compare(arrayListOf("value"), listOf("value")) { listBehavior() }
    }

    @test fun specialSets() {
        compare(linkedSetOf<Int>(), setOf<Int>()) { setBehavior() }
        compare(hashSetOf<Double>(), emptySet<Double>()) { setBehavior() }
        compare(listOf("value").toMutableSet(), setOf("value")) { setBehavior() }
    }

    @test fun specialMaps() {
        compare(hashMapOf<String, Int>(), mapOf<String, Int>()) { mapBehavior() }
        compare(linkedMapOf<Int, String>(), emptyMap<Int, String>()) { mapBehavior() }
        compare(linkedMapOf(2 to 3), mapOf(2 to 3)) { mapBehavior() }
    }

    @test fun toStringTest() {
        // we need toString() inside pattern because of KT-8666
        assertEquals("[1, a, null, ${Long.MAX_VALUE.toString()}]", listOf(1, "a", null, Long.MAX_VALUE).toString())
    }

    @test fun randomAccess() {
        assertTrue(arrayListOf(1) is RandomAccess, "ArrayList is RandomAccess")
        assertTrue(listOf(1, 2) is RandomAccess, "Default read-only list implementation is RandomAccess")
        assertTrue(listOf(1) is RandomAccess, "Default singleton list is RandomAccess")
        assertTrue(emptyList<Int>() is RandomAccess, "Empty list is RandomAccess")
    }
}
