package test.collections

import java.util.*
import kotlin.test.*
import org.junit.Test as test

class CollectionTest {

    test fun joinTo() {
        val data = listOf("foo", "bar")
        val buffer = StringBuilder()
        data.joinTo(buffer, "-", "{", "}")
        assertEquals("{foo-bar}", buffer.toString())
    }

    test fun join() {
        val data = listOf("foo", "bar")
        val text = data.join("-", "<", ">")
        assertEquals("<foo-bar>", text)

        val big = listOf("a", "b", "c", "d", "e", "f")
        val text2 = big.join(limit = 3, truncated = "*")
        assertEquals("a, b, c, *", text2)
    }

    test fun filterNotNull() {
        val data = listOf(null, "foo", null, "bar")
        val foo = data.filterNotNull()

        assertEquals(2, foo.size())
        assertEquals(listOf("foo", "bar"), foo)

        assertTrue {
            foo is List<String>
        }
    }

    test fun mapNotNull() {
        val data = listOf(null, "foo", null, "bar")
        val foo = data.mapNotNull { it.length() }
        assertEquals(2, foo.size())
        assertEquals(listOf(3, 3), foo)

        assertTrue {
            foo is List<Int>
        }
    }

    test fun filterIntoSet() {
        val data = listOf("foo", "bar")
        val foo = data.filterTo(hashSetOf<String>()) { it.startsWith("f") }

        assertTrue {
            foo.all { it.startsWith("f") }
        }
        assertEquals(1, foo.size())
        assertEquals(hashSetOf("foo"), foo)

        assertTrue {
            foo is HashSet<String>
        }
    }

    test fun fold() {
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

    test fun foldWithDifferentTypes() {
        expect(7) {
            val numbers = listOf("a", "ab", "abc")
            numbers.fold(1) { a, b -> a + b.length() }
        }

        expect("1234") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.fold("") { a, b -> a + b }
        }
    }

    test fun foldWithNonCommutativeOperation() {
        expect(1) {
            val numbers = listOf(1, 2, 3)
            numbers.fold(7) { a, b -> a - b }
        }
    }

    test fun foldRight() {
        expect("1234") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.map { it.toString() }.foldRight("") { a, b -> a + b }
        }
    }

    test fun foldRightWithDifferentTypes() {
        expect("1234") {
            val numbers = listOf(1, 2, 3, 4)
            numbers.foldRight("") { a, b -> "" + a + b }
        }
    }

    test fun foldRightWithNonCommutativeOperation() {
        expect(-5) {
            val numbers = listOf(1, 2, 3)
            numbers.foldRight(7) { a, b -> a - b }
        }
    }

    test
    fun merge() {
        expect(listOf("ab", "bc", "cd")) {
            listOf("a", "b", "c").merge(listOf("b", "c", "d")) { a, b -> a + b }
        }
    }

    test
    fun zip() {
        expect(listOf("a" to "b", "b" to "c", "c" to "d")) {
            listOf("a", "b", "c").zip(listOf("b", "c", "d"))
        }
    }

    test fun partition() {
        val data = listOf("foo", "bar", "something", "xyz")
        val pair = data.partition { it.length() == 3 }

        assertEquals(listOf("foo", "bar", "xyz"), pair.first, "pair.first")
        assertEquals(listOf("something"), pair.second, "pair.second")
    }

    test fun reduce() {
        expect("1234") {
            val list = listOf("1", "2", "3", "4")
            list.reduce { a, b -> a + b }
        }

        //        TODO replace with more accurate version when KT-5987 will be fixed
        //        failsWith(javaClass<UnsupportedOperationException>()) {
        fails {
            arrayListOf<Int>().reduce { a, b -> a + b }
        }
    }

    test fun reduceRight() {
        expect("1234") {
            val list = listOf("1", "2", "3", "4")
            list.reduceRight { a, b -> a + b }
        }

        //        TODO replace with more accurate version when KT-5987 will be fixed
        //        failsWith(javaClass<UnsupportedOperationException>()) {
        fails {
            arrayListOf<Int>().reduceRight { a, b -> a + b }
        }
    }

    test fun groupBy() {
        val words = listOf("a", "abc", "ab", "def", "abcd")
        val byLength = words.groupBy { it.length() }
        assertEquals(4, byLength.size())

        // verify that order of keys is preserved
        val listOfPairs = byLength.toList()
        assertEquals(1, listOfPairs[0].first)
        assertEquals(3, listOfPairs[1].first)
        assertEquals(2, listOfPairs[2].first)
        assertEquals(4, listOfPairs[3].first)

        val l3 = byLength.getOrElse(3, { ArrayList<String>() })
        assertEquals(2, l3.size())
    }

    test fun plusRanges() {
        val range1 = 1..3
        val range2 = 4..7
        val combined = range1 + range2
        assertEquals((1..7).toList(), combined)
    }

    test fun mapRanges() {
        val range = 1..3 map { it * 2 }
        assertEquals(listOf(2, 4, 6), range)
    }

    test fun plus() {
        val list = listOf("foo", "bar")
        val list2 = list + "cheese"
        assertEquals(listOf("foo", "bar"), list)
        assertEquals(listOf("foo", "bar", "cheese"), list2)

        // lets use a mutable variable
        var list3 = listOf("a", "b")
        list3 += "c"
        assertEquals(listOf("a", "b", "c"), list3)
    }

    test fun plusCollectionBug() {
        val list = listOf("foo", "bar") + listOf("cheese", "wine")
        assertEquals(listOf("foo", "bar", "cheese", "wine"), list)
    }

    test fun plusCollection() {
        val a = listOf("foo", "bar")
        val b = listOf("cheese", "wine")
        val list = a + b
        assertEquals(listOf("foo", "bar", "cheese", "wine"), list)

        // lets use a mutable variable
        var ml: List<String> = a
        ml += "beer"
        ml += b
        ml += "z"
        assertEquals(listOf("foo", "bar", "beer", "cheese", "wine", "z"), ml)
    }

    test fun requireNoNulls() {
        val data = arrayListOf<String?>("foo", "bar")
        val notNull = data.requireNoNulls()
        assertEquals(listOf("foo", "bar"), notNull)

        val hasNulls = listOf("foo", null, "bar")

        //        TODO replace with more accurate version when KT-5987 will be fixed
        //        failsWith(javaClass<IllegalArgumentException>()) {
        fails {
            // should throw an exception as we have a null
            hasNulls.requireNoNulls()
        }
    }

    test fun reverse() {
        val data = listOf("foo", "bar")
        val rev = data.reverse()
        assertEquals(listOf("bar", "foo"), rev)
    }

    test fun reverseFunctionShouldReturnReversedCopyForList() {
        val list: List<Int> = listOf(2, 3, 1)
        expect(listOf(1, 3, 2)) { list.reverse() }
        expect(listOf(2, 3, 1)) { list }
    }

    test fun reverseFunctionShouldReturnReversedCopyForIterable() {
        val iterable: Iterable<Int> = listOf(2, 3, 1)
        expect(listOf(1, 3, 2)) { iterable.reverse() }
        expect(listOf(2, 3, 1)) { iterable }
    }


    test fun drop() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(listOf("bar", "abc"), coll.drop(1))
        assertEquals(listOf("abc"), coll.drop(2))
    }

    test fun dropWhile() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(listOf("bar", "abc"), coll.dropWhile { it.startsWith("f") })
    }

    test fun take() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(listOf("foo"), coll.take(1))
        assertEquals(listOf("foo", "bar"), coll.take(2))
    }

    test fun takeWhile() {
        val coll = listOf("foo", "bar", "abc")
        assertEquals(listOf("foo"), coll.takeWhile { it.startsWith("f") })
        assertEquals(listOf("foo", "bar", "abc"), coll.takeWhile { it.length() == 3 })
    }

    test fun copyToArray() {
        val data = listOf("foo", "bar")
        val arr = data.copyToArray()
        println("Got array ${arr}")
        assertEquals(2, arr.size())
        todo {
            assertTrue {
                arr is Array<String>
            }
        }
    }

    test fun count() {
        val data = listOf("foo", "bar")
        assertEquals(2, data.count())
        assertEquals(3, hashSetOf(12, 14, 15).count())
        assertEquals(0, ArrayList<Double>().count())
    }

    test fun first() {
        val data = listOf("foo", "bar")
        assertEquals("foo", data.first())
        assertEquals(15, listOf(15, 19, 20, 25).first())
        assertEquals('a', listOf('a').first())
        fails { arrayListOf<Int>().first() }
    }

    test fun last() {
        val data = listOf("foo", "bar")
        assertEquals("bar", data.last())
        assertEquals(25, listOf(15, 19, 20, 25).last())
        assertEquals('a', listOf('a').last())
        fails { arrayListOf<Int>().last() }
    }

    test fun subscript() {
        val list = arrayListOf("foo", "bar")
        assertEquals("foo", list[0])
        assertEquals("bar", list[1])

        // lists throw an exception if out of range
        fails {
            assertEquals(null, list[2])
        }

        // lets try update the list
        list[0] = "new"
        list[1] = "thing"

        // lists don't allow you to set past the end of the list
        fails {
            list[2] = "works"
        }

        list.add("works")
        assertEquals(listOf("new", "thing", "works"), list)
    }

    test fun indices() {
        val data = listOf("foo", "bar")
        val indices = data.indices
        assertEquals(0, indices.start)
        assertEquals(1, indices.end)
        assertEquals(indices, data.size().indices)
    }

    test fun contains() {
        assertFalse(hashSetOf<Int>().contains(12))
        assertTrue(listOf(15, 19, 20).contains(15))

        assertTrue(IterableWrapper(hashSetOf(45, 14, 13)).contains(14))
    }

    test fun sortForMutableIterable() {
        val list: MutableIterable<Int> = arrayListOf(2, 3, 1)
        expect(listOf(1, 2, 3)) { list.sort() }
        expect(listOf(2, 3, 1)) { list }
    }

    test fun sortForIterable() {
        val list: Iterable<Int> = listOf(2, 3, 1)
        expect(listOf(1, 2, 3)) { list.sort() }
        expect(listOf(2, 3, 1)) { list }
    }

    test fun min() {
        expect(null, { listOf<Int>().min() })
        expect(1, { listOf(1).min() })
        expect(2, { listOf(2, 3).min() })
        expect(2000000000000, { listOf(3000000000000, 2000000000000).min() })
        expect('a', { listOf('a', 'b').min() })
        expect("a", { listOf("a", "b").min() })
        expect(null, { listOf<Int>().sequence().min<Int>() })
        expect(2, { listOf(2, 3).sequence().min<Int>() })
    }

    test fun max() {
        expect(null, { listOf<Int>().max() })
        expect(1, { listOf(1).max() })
        expect(3, { listOf(2, 3).max() })
        expect(3000000000000, { listOf(3000000000000, 2000000000000).max() })
        expect('b', { listOf('a', 'b').max() })
        expect("b", { listOf("a", "b").max() })
        expect(null, { listOf<Int>().sequence().max<Int>() })
        expect(3, { listOf(2, 3).sequence().max<Int>() })
    }

    test fun minBy() {
        expect(null, { listOf<Int>().minBy { it } })
        expect(1, { listOf(1).minBy { it } })
        expect(3, { listOf(2, 3).minBy { -it } })
        expect('a', { listOf('a', 'b').minBy { "x$it" } })
        expect("b", { listOf("b", "abc").minBy { it.length() } })
        expect(null, { listOf<Int>().sequence().minBy<Int, Int> { it } })
        expect(3, { listOf(2, 3).sequence().minBy<Int, Int> { -it } })
    }

    test fun maxBy() {
        expect(null, { listOf<Int>().maxBy { it } })
        expect(1, { listOf(1).maxBy { it } })
        expect(2, { listOf(2, 3).maxBy { -it } })
        expect('b', { listOf('a', 'b').maxBy { "x$it" } })
        expect("abc", { listOf("b", "abc").maxBy { it.length() } })
        expect(null, { listOf<Int>().sequence().maxBy<Int, Int> { it } })
        expect(2, { listOf(2, 3).sequence().maxBy<Int, Int> { -it } })
    }

    test fun minByEvaluateOnce() {
        var c = 0
        expect(1, { listOf(5, 4, 3, 2, 1).minBy { c++; it * it } })
        assertEquals(5, c)
        c = 0
        expect(1, { listOf(5, 4, 3, 2, 1).sequence().minBy<Int, Int> { c++; it * it } })
        assertEquals(5, c)
    }

    test fun maxByEvaluateOnce() {
        var c = 0
        expect(5, { listOf(5, 4, 3, 2, 1).maxBy { c++; it * it } })
        assertEquals(5, c)
        c = 0
        expect(5, { listOf(5, 4, 3, 2, 1).sequence().maxBy<Int, Int> { c++; it * it } })
        assertEquals(5, c)
    }

    test fun sum() {
        expect(0) { arrayListOf<Int>().sum() }
        expect(14) { listOf(2, 3, 9).sum() }
        expect(3.0) { listOf(1.0, 2.0).sum() }
        expect(3000000000000) { arrayListOf<Long>(1000000000000, 2000000000000).sum() }
        expect(3.0.toFloat()) { arrayListOf<Float>(1.0.toFloat(), 2.0.toFloat()).sum() }
        expect(3.0.toFloat()) { sequenceOf<Float>(1.0.toFloat(), 2.0.toFloat()).sum() }
    }

    test fun takeReturnsFirstNElements() {
        expect(listOf(1, 2, 3, 4, 5)) { (1..10) take 5 }
        expect(listOf(1, 2, 3, 4, 5)) { (1..10).toList().take(5) }
        expect(listOf(1, 2)) { (1..10) take 2 }
        expect(listOf(1, 2)) { (1..10).toList().take(2) }
        expect(true) { (0L..5L).take(0).none() }
        expect(true) { listOf(1L).take(0).none() }
        expect(listOf(1)) { (1..1) take 10 }
        expect(listOf(1)) { listOf(1) take 10 }
    }

    test fun sortBy() {
        expect(listOf("two" to 2, "three" to 3)) {
            listOf("three" to 3, "two" to 2).sortBy { it.second }
        }
        expect(listOf("three" to 3, "two" to 2)) {
            listOf("three" to 3, "two" to 2).sortBy { it.first }
        }
        expect(listOf("two" to 2, "three" to 3)) {
            listOf("three" to 3, "two" to 2).sortBy { it.first.length() }
        }
    }

    test fun sortFunctionShouldReturnSortedCopyForList() {
        val list: List<Int> = listOf(2, 3, 1)
        expect(listOf(1, 2, 3)) { list.sort() }
        expect(listOf(2, 3, 1)) { list }
    }

    test fun sortFunctionShouldReturnSortedCopyForIterable() {
        val list: Iterable<Int> = listOf(2, 3, 1)
        expect(listOf(1, 2, 3)) { list.sort() }
        expect(listOf(2, 3, 1)) { list }
    }

    test fun decomposeFirst() {
        val (first) = listOf(1, 2)
        assertEquals(first, 1)
    }

    test fun decomposeSplit() {
        val (key, value) = "key = value".split("=").map { it.trim() }
        assertEquals(key, "key")
        assertEquals(value, "value")
    }

    test fun decomposeList() {
        val (a, b, c, d, e) = listOf(1, 2, 3, 4, 5)
        assertEquals(a, 1)
        assertEquals(b, 2)
        assertEquals(c, 3)
        assertEquals(d, 4)
        assertEquals(e, 5)
    }

    test fun decomposeArray() {
        val (a, b, c, d, e) = array(1, 2, 3, 4, 5)
        assertEquals(a, 1)
        assertEquals(b, 2)
        assertEquals(c, 3)
        assertEquals(d, 4)
        assertEquals(e, 5)
    }

    test fun decomposeIntArray() {
        val (a, b, c, d, e) = intArray(1, 2, 3, 4, 5)
        assertEquals(a, 1)
        assertEquals(b, 2)
        assertEquals(c, 3)
        assertEquals(d, 4)
        assertEquals(e, 5)
    }
}
