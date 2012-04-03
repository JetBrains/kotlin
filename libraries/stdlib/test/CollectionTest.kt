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

    test fun filterNot() {
        val data = arrayList("foo", "bar")
        val foo = data.filterNot{it.startsWith("b")}

        assertTrue {
            foo.all{it.startsWith("f")}
        }
        assertEquals(1, foo.size)
        assertEquals(arrayList("foo"), foo)
    }

    // TODO would be nice to avoid the <String>
    test fun filterIntoLinkedList() {
        val data = arrayList("foo", "bar")
        val foo = data.filterTo(linkedList<String>()){it.startsWith("f")}

        assertTrue {
            foo.all{it.startsWith("f")}
        }
        assertEquals(1, foo.size)
        assertEquals(linkedList("foo"), foo)

        assertTrue {
            foo is LinkedList<String>
        }
    }

    // TODO would be nice to avoid the <String>
    test fun filterNotIntoLinkedList() {
        val data = arrayList("foo", "bar")
        val foo = data.filterNotTo(linkedList<String>()){it.startsWith("f")}

        assertTrue {
            foo.all{!it.startsWith("f")}
        }
        assertEquals(1, foo.size)
        assertEquals(linkedList("bar"), foo)

        assertTrue {
            foo is LinkedList<String>
        }
    }

    // TODO would be nice to avoid the <String>
    test fun filterNotNullIntoLinkedList() {
        val data = arrayList(null, "foo", null, "bar")
        val foo = data.filterNotNullTo(linkedList<String>())

        assertEquals(2, foo.size)
        assertEquals(linkedList("foo", "bar"), foo)

        assertTrue {
            foo is LinkedList<String>
        }
    }

    test fun filterNotNull() {
        val data = arrayList(null, "foo", null, "bar")
        val foo = data.filterNotNull()

        assertEquals(2, foo.size)
        assertEquals(linkedList("foo", "bar"), foo)

        assertTrue {
            foo is List<String>
        }
    }

    test fun filterIntoSet() {
        val data = arrayList("foo", "bar")
        // TODO would be nice to avoid the <String>
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

    test fun filterIntoSortedSet() {
        val data = arrayList("foo", "bar")
        // TODO would be nice to avoid the <String>
        val sorted = data.filterTo(sortedSet<String>()){it.length == 3}
        assertEquals(2, sorted.size)
        assertEquals(sortedSet("bar", "foo"), sorted)
        assertTrue {
            sorted is TreeSet<String>
        }
    }

    test fun find() {
        val data = arrayList("foo", "bar")
        val x = data.find{it.startsWith("x")}
        assertNull(x)
        fails {
            x.sure()
        }

        val f = data.find{it.startsWith("f")}
        f.sure()
        assertEquals("foo", f)
    }

    test fun flatMap() {
        val data = arrayList("", "foo", "bar", "x", "")
        val characters = data.flatMap<String,Character>{ it.toCharList() }
        println("Got list of characters ${characters}")
        assertEquals(7, characters.size())
        val text = characters.makeString("")
        assertEquals("foobarx", text)
    }

    test fun forEach() {
        val data = arrayList("foo", "bar")
        var count = 0
        data.forEach{ count += it.length }
        assertEquals(6, count)
    }


    /*
        // TODO would be nice to be able to write this as this
        //numbers.fold(0){it + it2}
        numbers.fold(0){(it, it2) -> it + it2}

        // TODO would be nice to be able to write this as this
        // numbers.map{it.toString()}.fold(""){it + it2}
        numbers.map<Int, String>{it.toString()}.fold(""){(it, it2) -> it + it2}
    */
    test fun fold() {
        // lets calculate the sum of some numbers
        expect(10) {
            val numbers = arrayList(1, 2, 3, 4)
            numbers.fold(0){(it, it2) -> it + it2}
        }

        expect(0) {
            val numbers = arrayList<Int>()
            numbers.fold(0){(it, it2) -> it + it2}
        }

        // lets concatenate some strings
        expect("1234") {
            val numbers = arrayList(1, 2, 3, 4)
            numbers.map<Int, String>{it.toString()}.fold(""){(it, it2) -> it + it2}
        }
    }

    /*
        // TODO would be nice to be able to write this as this
        // numbers.map{it.toString()}.foldRight(""){it + it2}
        numbers.map<Int, String>{it.toString()}.foldRight(""){(it, it2) -> it + it2}
    */
    test fun foldRight() {
        expect("4321") {
            val numbers = arrayList(1, 2, 3, 4)
            numbers.map<Int, String>{it.toString()}.foldRight(""){(it, it2) -> it + it2}
        }
    }

    /*
        TODO inference engine should not need this type info?
        val byLength = words.groupBy<String, Int>{it.length}
    */
    test fun groupBy() {
        val words = arrayList("a", "ab", "abc", "def", "abcd")
        val byLength = words.groupBy<String, Int>{it.length}
        assertEquals(4, byLength.size())

        val l3 = byLength.getOrElse(3, {ArrayList<String>()})
        assertEquals(2, l3.size)

    }


    test fun makeString() {
        val data = arrayList("foo", "bar")
        val text = data.makeString("-", "<", ">")
        assertEquals("<foo-bar>", text)
    }

    /*
        TODO compiler bug
        we should be able to remove the explicit type <String,Int> on the map function
        http://youtrack.jetbrains.net/issue/KT-1145
    */
    test fun map() {
        val data = arrayList("foo", "bar")
        val lengths = data.map<String, Int>{ it.length }
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

    /*
     TODO compiler fails on this one
     KT-1718
    test fun plusCollectionBug() {
        val list = arrayList("foo", "bar") + arrayList("cheese", "wine")
        assertEquals(arrayList("foo", "bar", "cheese", "wine"), list)
    }
    */

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
        failsWith<IllegalArgumentException> {
            // should throw an exception as we have a null
            hasNulls.requireNoNulls()
        }
    }

    test fun reverse() {
        val data = arrayList("foo", "bar")
        val rev = data.reverse()
        assertEquals(arrayList("bar", "foo"), rev)
    }

    test fun sort() {
        val coll: List<String> = arrayList("foo", "bar", "abc")

        // TODO fixme
        // Some sort of in/out variance thing - or an issue with Java interop?
        //coll.sort()
        todo {
            assertEquals(3, coll.size)
            assertEquals(arrayList("abc", "bar", "foo"), coll)

        }
    }

    test fun toArray() {
        val data = arrayList("foo", "bar")
        val arr = data.toArray()
        println("Got array ${arr}")
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

    test fun last() {
        val data = arrayList("foo", "bar")
        assertEquals("bar", data.last())
        assertEquals(25, arrayList(15, 19, 20, 25).last())
        assertEquals('a', linkedList('a').last())
    }
        // TODO
        // assertEquals(19, TreeSet(arrayList(90, 47, 19)).first())


    test fun lastException() {
        fails { arrayList<Int>().last() }
        fails { linkedList<String>().last() }
        fails { hashSet<Char>().last() }
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
        assertEquals(2, indices.size)
        assertFalse(indices.isReversed)
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
        assertTrue(linkedList(15, 19, 20).contains(15))

        //    assertTrue(IterableWrapper(hashSet(45, 14, 13)).contains(14))
        //    assertFalse(IterableWrapper(linkedList<Int>()).contains(15))
    }


    class IterableWrapper<T>(collection : java.lang.Iterable<T>) : java.lang.Iterable<T> {
        private val collection = collection

        override fun iterator(): java.util.Iterator<T> {
            return collection.iterator().sure()
        }
    }
}
