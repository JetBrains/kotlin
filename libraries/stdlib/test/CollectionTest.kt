package test.collections

import kotlin.test.*

import java.util.*

import org.junit.Test as test

class CollectionTest {

    test fun all() {
        val data = arrayList("foo", "bar")
        assertTrue {
            data.all{it.length == 3}
        }
        assertNot {
            data.all{s -> s.startsWith("b")}
        }
    }

    test fun any() {
        val data = arrayList("foo", "bar")
        assertTrue {
            data.any{it.startsWith("f")}
        }
        assertNot {
            data.any{it.startsWith("x")}
        }
    }


    test fun appendString() {
        val data = arrayList("foo", "bar")
        val buffer = StringBuilder()
        val text = data.appendString(buffer, "-", "{", "}")
        assertEquals("{foo-bar}", buffer.toString())
    }

    test fun count() {
        val data = arrayList("foo", "bar")
        assertEquals(1, data.count{it.startsWith("b")})
        assertEquals(2, data.count{it.size == 3})
    }

    test fun filter() {
        val data = arrayList("foo", "bar")
        val foo = data.filter{it.startsWith("f")}
        assertTrue {
            foo.all{it.startsWith("f")}
        }
        assertEquals(1, foo.size)
        assertEquals(arrayList("foo"), foo)
    }

    test fun filterReturnsList() {
        val data = arrayList("foo", "bar")
        val foo = data.filter{it.startsWith("f")}
        assertTrue {
            foo is List<String>
        }
    }

    test fun filterNot() {
        val data = arrayList("foo", "bar")
        val foo = data.filterNot{it.startsWith("b")}

        assertTrue {
            foo.all{it.startsWith("f")}
        }
        assertEquals(1, foo.size)
        assertEquals(arrayList("foo"), foo)
    }

    test fun filterNotNull() {
        val data = arrayList(null, "foo", null, "bar")
        val foo = data.filterNotNull()

        assertEquals(2, foo.size)
        assertEquals(arrayList("foo", "bar"), foo)

        assertTrue {
            foo is List<String>
        }
    }

    // TODO would be nice to avoid the <String>
    test fun filterIntoSet() {
        val data = arrayList("foo", "bar")
        val foo = data.filterTo(hashSet<String>()){it.startsWith("f")}

        assertTrue {
            foo.all{it.startsWith("f")}
        }
        assertEquals(1, foo.size)
        assertEquals(hashSet("foo"), foo)

        assertTrue {
            foo is HashSet<String>
        }
    }

    test fun find() {
        val data = arrayList("foo", "bar")
        val x = data.find{it.startsWith("x")}
        assertNull(x)

        val f = data.find{it.startsWith("f")}
        f!!
        assertEquals("foo", f)
    }

    test fun forEach() {
        val data = arrayList("foo", "bar")
        var count = 0
        data.forEach{ count += it.length }
        assertEquals(6, count)
    }

    test fun fold() {
        // lets calculate the sum of some numbers
        expect(10) {
            val numbers = arrayList(1, 2, 3, 4)
            numbers.fold(0){ a, b -> a + b}
        }

        expect(0) {
            val numbers = arrayList<Int>()
            numbers.fold(0){ a, b -> a + b}
        }

        // lets concatenate some strings
        expect("1234") {
            val numbers = arrayList(1, 2, 3, 4)
            numbers.map{it.toString()}.fold(""){ a, b -> a + b}
        }
    }

    test fun foldWithDifferentTypes() {
        expect(7) {
            val numbers = arrayList("a", "ab", "abc")
            numbers.fold(1){ a, b -> a + b.size}
        }

        expect("1234") {
            val numbers = arrayList(1, 2, 3, 4)
            numbers.fold(""){ a, b -> a + b}
        }
    }

    test fun foldWithNonCommutativeOperation() {
        expect(1) {
            val numbers = arrayList(1, 2, 3)
            numbers.fold(7) {a, b -> a - b}
        }
    }

    test fun foldRight() {
        expect("1234") {
            val numbers = arrayList(1, 2, 3, 4)
            numbers.map{it.toString()}.foldRight(""){ a, b -> a + b}
        }
    }

    test fun foldRightWithDifferentTypes() {
        expect("1234") {
            val numbers = arrayList(1, 2, 3, 4)
            numbers.foldRight(""){ a, b -> "" + a + b}
        }
    }

    test fun foldRightWithNonCommutativeOperation() {
        expect(-5) {
            val numbers = arrayList(1, 2, 3)
            numbers.foldRight(7) {a, b -> a - b}
        }
    }

    test fun partition() {
        val data = arrayList("foo", "bar", "something", "xyz")
        val pair = data.partition{it.size == 3}

        assertEquals(arrayList("foo", "bar", "xyz"), pair.first, "pair.first")
        assertEquals(arrayList("something"), pair.second, "pair.second")
    }

    test fun reduce() {
        expect("1234") {
            val list = arrayList("1", "2", "3", "4")
            list.reduce { a, b -> a + b }
        }

        failsWith(javaClass<UnsupportedOperationException>()) {
            arrayList<Int>().reduce { a, b -> a + b}
        }
    }

    test fun reduceRight() {
        expect("1234") {
            val list = arrayList("1", "2", "3", "4")
            list.reduceRight { a, b -> a + b }
        }

        failsWith(javaClass<UnsupportedOperationException>()) {
            arrayList<Int>().reduceRight { a, b -> a + b}
        }
    }

    test fun groupBy() {
        val words = arrayList("a", "ab", "abc", "def", "abcd")
        val byLength = words.groupBy{ it.length }
        assertEquals(4, byLength.size())

        val l3 = byLength.getOrElse(3, {ArrayList<String>()})
        assertEquals(2, l3.size)
    }

    test fun makeString() {
        val data = arrayList("foo", "bar")
        val text = data.makeString("-", "<", ">")
        assertEquals("<foo-bar>", text)

        val big = arrayList("a", "b", "c", "d" , "e", "f")
        val text2 = big.makeString(limit = 3, truncated = "*")
        assertEquals("a, b, c, *", text2)
    }

    test fun map() {
        val data = arrayList("foo", "bar")
        val lengths = data.map{ it.length }
        assertTrue {
            lengths.all{it == 3}
        }
        assertEquals(2, lengths.size)
        assertEquals(arrayList(3, 3), lengths)
    }

    test fun plus() {
        val list = arrayList("foo", "bar")
        val list2 = list + "cheese"
        assertEquals(arrayList("foo", "bar"), list)
        assertEquals(arrayList("foo", "bar", "cheese"), list2)

        // lets use a mutable variable
        var list3 = arrayList("a", "b")
        list3 += "c"
        assertEquals(arrayList("a", "b", "c"), list3)
    }

    test fun plusCollectionBug() {
        val list = arrayList("foo", "bar") + arrayList("cheese", "wine")
        assertEquals(arrayList("foo", "bar", "cheese", "wine"), list)
    }

    test fun plusCollection() {
        val a = arrayList("foo", "bar")
        val b = arrayList("cheese", "wine")
        val list = a + b
        assertEquals(arrayList("foo", "bar", "cheese", "wine"), list)

        // lets use a mutable variable
        var ml = a
        ml += "beer"
        ml += b
        ml += "z"
        assertEquals(arrayList("foo", "bar", "beer", "cheese", "wine", "z"), ml)
    }

    test fun requireNoNulls() {
        val data = arrayList<String?>("foo", "bar")
        val notNull = data.requireNoNulls()
        assertEquals(arrayList("foo", "bar"), notNull)

        val hasNulls = arrayList("foo", null, "bar")
        failsWith(javaClass<IllegalArgumentException>()) {
            // should throw an exception as we have a null
            hasNulls.requireNoNulls()
        }
    }

    test fun reverse() {
        val data = arrayList("foo", "bar")
        val rev = data.reverse()
        assertEquals(arrayList("bar", "foo"), rev)
    }

    test fun reverseFunctionShouldReturnReversedCopyForList() {
        val list : List<Int> = arrayList(2, 3, 1)
        expect(arrayList(1, 3, 2)) { list.reverse() }
        expect(arrayList(2, 3, 1)) { list }
    }

    test fun reverseFunctionShouldReturnReversedCopyForIterable() {
        val iterable : Iterable<Int> = arrayList(2, 3, 1)
        expect(arrayList(1, 3, 2)) { iterable.reverse() }
        expect(arrayList(2, 3, 1)) { iterable }
    }


    test fun drop() {
        val coll = arrayList("foo", "bar", "abc")
        assertEquals(arrayList("bar", "abc"), coll.drop(1))
        assertEquals(arrayList("abc"), coll.drop(2))
    }

    test fun dropWhile() {
        val coll = arrayList("foo", "bar", "abc")
        assertEquals(arrayList("bar", "abc"), coll.dropWhile{ it.startsWith("f") })
    }

    test fun take() {
        val coll = arrayList("foo", "bar", "abc")
        assertEquals(arrayList("foo"), coll.take(1))
        assertEquals(arrayList("foo", "bar"), coll.take(2))
    }

    test fun takeWhile() {
        val coll = arrayList("foo", "bar", "abc")
        assertEquals(arrayList("foo"), coll.takeWhile{ it.startsWith("f") })
        assertEquals(arrayList("foo", "bar", "abc"), coll.takeWhile{ it.size == 3 })
    }

    test fun toArray() {
        val data = arrayList("foo", "bar")
        val arr = data.toArray()
        println("Got array ${arr}")
        assertEquals(2, arr.size)
        todo {
            assertTrue {
                arr is Array<String>
            }
        }
    }

    test fun simpleCount() {
        val data = arrayList("foo", "bar")
        assertEquals(2, data.count())
        assertEquals(3, hashSet(12, 14, 15).count())
        assertEquals(0, ArrayList<Double>().count())
    }

    //todo after KT-1873 the name might be returned to 'last'
    test fun lastElement() {
        val data = arrayList("foo", "bar")
        assertEquals("bar", data.last())
        assertEquals(25, arrayList(15, 19, 20, 25).last())
        assertEquals('a', arrayList('a').last())
    }
        // TODO
        // assertEquals(19, TreeSet(arrayList(90, 47, 19)).first())


    test fun lastException() {
        fails { arrayList<Int>().last() }
    }

    test fun subscript() {
        val list = arrayList("foo", "bar")
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
        assertEquals(arrayList("new", "thing", "works"), list)
    }

    test fun indices() {
        val data = arrayList("foo", "bar")
        val indices = data.indices
        assertEquals(0, indices.start)
        assertEquals(1, indices.end)

        assertEquals(indices, data.size. indices)
    }

    test fun contains() {
        val data = arrayList("foo", "bar")
        assertTrue(data.contains("foo"))
        assertTrue(data.contains("bar"))
        assertFalse(data.contains("some"))

        // TODO: Problems with generation
        //    assertTrue(IterableWrapper(data).contains("bar"))
        //    assertFalse(IterableWrapper(data).contains("some"))

        assertFalse(hashSet<Int>().contains(12))
        assertTrue(arrayList(15, 19, 20).contains(15))

        //    assertTrue(IterableWrapper(hashSet(45, 14, 13)).contains(14))
        //    assertFalse(IterableWrapper(linkedList<Int>()).contains(15))
    }

    test fun sortForMutableIterable() {
        val list : MutableIterable<Int> = arrayList<Int>(2, 3, 1)
        expect(arrayList(1, 2, 3)) { list.sort() }
        expect(arrayList(2, 3, 1)) { list }
    }

    test fun sortForIterable() {
        val list : Iterable<Int> = listOf(2, 3, 1)
        expect(arrayList(1, 2, 3)) { list.sort() }
        expect(arrayList(2, 3, 1)) { list }
    }

    test fun min() {
        expect(null, { listOf<Int>().min() })
        expect(1, { listOf(1).min() })
        expect(2, { listOf(2, 3).min() })
        expect(2000000000000, { listOf(3000000000000, 2000000000000).min() })
        expect('a', { listOf('a', 'b').min() })
        expect("a", { listOf("a", "b").min() })
        expect(null, { listOf<Int>().iterator().min() })
        expect(2, { listOf(2, 3).iterator().min() })
    }

    test fun max() {
        expect(null, { listOf<Int>().max() })
        expect(1, { listOf(1).max() })
        expect(3, { listOf(2, 3).max() })
        expect(3000000000000, { listOf(3000000000000, 2000000000000).max() })
        expect('b', { listOf('a', 'b').max() })
        expect("b", { listOf("a", "b").max() })
        expect(null, { listOf<Int>().iterator().max() })
        expect(3, { listOf(2, 3).iterator().max() })
    }

    test fun minBy() {
        expect(null, { listOf<Int>().minBy { it } })
        expect(1, { listOf(1).minBy { it } })
        expect(3, { listOf(2, 3).minBy { -it } })
        expect('a', { listOf('a', 'b').minBy { "x$it" } })
        expect("b", { listOf("b", "abc").minBy { it.length } })
        expect(null, { listOf<Int>().iterator().minBy { it } })
        expect(3, { listOf(2, 3).iterator().minBy { -it } })
    }

    test fun maxBy() {
        expect(null, { listOf<Int>().maxBy { it } })
        expect(1, { listOf(1).maxBy { it } })
        expect(2, { listOf(2, 3).maxBy { -it } })
        expect('b', { listOf('a', 'b').maxBy { "x$it" } })
        expect("abc", { listOf("b", "abc").maxBy { it.length } })
        expect(null, { listOf<Int>().iterator().maxBy { it } })
        expect(2, { listOf(2, 3).iterator().maxBy { -it } })
    }

    test fun minByEvaluateOnce() {
        var c = 0
        expect(1, { listOf(5, 4, 3, 2, 1).minBy { c++; it * it } })
        assertEquals(5, c)
        c = 0
        expect(1, { listOf(5, 4, 3, 2, 1).iterator().minBy { c++; it * it } })
        assertEquals(5, c)
    }

    test fun maxByEvaluateOnce() {
        var c = 0
        expect(5, { listOf(5, 4, 3, 2, 1).maxBy { c++; it * it } })
        assertEquals(5, c)
        c = 0
        expect(5, { listOf(5, 4, 3, 2, 1).iterator().maxBy { c++; it * it } })
        assertEquals(5, c)
    }

    test fun sum() {
        expect(0) { ArrayList<Int>().sum() }
        expect(14) { arrayListOf(2, 3, 9).sum() }
        expect(3.0) { arrayListOf(1.0, 2.0).sum() }
        expect(3000000000000) { arrayListOf<Long>(1000000000000, 2000000000000).sum() }
        expect(3.0) { arrayListOf<Float>(1.0, 2.0).sum() }
    }

    class IterableWrapper<T>(collection : Iterable<T>) : Iterable<T> {
        private val collection = collection

        override fun iterator(): Iterator<T> {
            return collection.iterator()
        }
    }
}
