package test.collections

import kotlin.test.*

import java.util.*

import org.junit.Test

class CollectionTest {

    class IterableWrapper<T>(collection : java.lang.Iterable<T>) : java.lang.Iterable<T> {
        private val collection = collection

        override fun iterator(): java.util.Iterator<T> {
            return collection.iterator().sure()
        }
    }


    val data = arrayList("foo", "bar")

    Test fun any() {
        assertTrue {
            data.any{it.startsWith("f")}
        }
        assertNot {
            data.any{it.startsWith("x")}
        }
    }

    Test fun all() {
        assertTrue {
            data.all{it.length == 3}
        }
        assertNot {
            data.all{s -> s.startsWith("b")}
        }
    }

    Test fun count() {
        assertEquals(1, data.count{it.startsWith("b")})
        assertEquals(2, data.count{it.size == 3})
    }

    Test fun filter() {
        val foo = data.filter{it.startsWith("f")}

        assertTrue {
            foo.all{it.startsWith("f")}
        }
        assertEquals(1, foo.size)
        assertEquals(arrayList("foo"), foo)
    }

    Test fun filterNot() {
        val foo = data.filterNot{it.startsWith("b")}

        assertTrue {
            foo.all{it.startsWith("f")}
        }
        assertEquals(1, foo.size)
        assertEquals(arrayList("foo"), foo)
    }

    Test fun filterIntoLinkedList() {
        // TODO would be nice to avoid the <String>
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

    Test fun filterIntoSet() {
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

    Test fun filterIntoSortedSet() {
        // TODO would be nice to avoid the <String>
        val sorted = data.filterTo(sortedSet<String>()){it.length == 3}
        assertEquals(2, sorted.size)
        assertEquals(sortedSet("bar", "foo"), sorted)
        assertTrue {
            sorted is TreeSet<String>
        }
    }

    Test fun find() {
        val x = data.find{it.startsWith("x")}
        assertNull(x)
        fails {
            x.sure()
        }

        val f = data.find{it.startsWith("f")}
        f.sure()
        assertEquals("foo", f)
    }

    Test fun flatMap() {
        val characters = arrayList('f', 'o', 'o', 'b', 'a', 'r')
        // TODO figure out how to get a line like this to compile :)
        /*
        val characters = data.flatMap<String,Character>{
          it.toCharArray().toList() as Collection<Character>
        }
        */
        todo {
            println("Got list of characters ${characters}")
            val text = characters.join("")
            assertEquals("foobar", text)
        }
    }

    Test fun foreach() {
        var count = 0
        data.forEach{ count += it.length }
        assertEquals(6, count)
    }

    Test fun fold() {
        expect(10) {
            val numbers = arrayList(1, 2, 3, 4)

            // TODO would be nice to be able to write this as this
            //numbers.fold(0){it + it2}
            numbers.fold(0){(it, it2) -> it + it2}
        }

        expect(0) {
            val numbers = arrayList<Int>()
            numbers.fold(0){(it, it2) -> it + it2}
        }

        expect("1234") {
            val numbers = arrayList(1, 2, 3, 4)

            // TODO would be nice to be able to write this as this
            // numbers.map{it.toString()}.fold(""){it + it2}
            numbers.map<Int, String>{it.toString()}.fold(""){(it, it2) -> it + it2}
        }
    }

    Test fun foldRight() {
        expect("4321") {
            val numbers = arrayList(1, 2, 3, 4)

            // TODO would be nice to be able to write this as this
            // numbers.map{it.toString()}.foldRight(""){it + it2}
            numbers.map<Int, String>{it.toString()}.foldRight(""){(it, it2) -> it + it2}
        }
    }


    Test fun groupBy() {
        val words = arrayList("a", "ab", "abc", "def", "abcd")
        /*
        TODO inference engine should not need this type info?
        */
        val byLength = words.groupBy<String, Int>{it.length}
        assertEquals(4, byLength.size())

        println("Grouped by length is: $byLength")
        /*
         TODO compiler bug...

        val l3 = byLength.getOrElse(3, {ArrayList<String>()})
        assertEquals(2, l3.size)
        */

    }

    Test fun join() {
        val text = data.join("-", "<", ">")
        assertEquals("<foo-bar>", text)
    }

    Test fun map() {
        /**
          TODO compiler bug
          we should be able to remove the explicit type on the function
          http://youtrack.jetbrains.net/issue/KT-1145
        */
        val lengths = data.map<String, Int>{s -> s.length}
        assertTrue {
            lengths.all{it == 3}
        }
        assertEquals(2, lengths.size)
        assertEquals(arrayList(3, 3), lengths)
    }

    Test fun reverse() {
        val rev = data.reverse()
        assertEquals(arrayList("bar", "foo"), rev)
    }

    Test fun sort() {
        val coll: List<String> = arrayList("foo", "bar", "abc")

        // TODO fixme
        // Some sort of in/out variance thing - or an issue with Java interop?
        //coll.sort()
        todo {
            assertEquals(3, coll.size)
            assertEquals(arrayList("abc", "bar", "foo"), coll)

        }
    }

    Test fun toArray() {
        val arr = data.toArray()
        println("Got array ${arr}")
        todo {
            assertTrue {
                arr is Array<String>
            }
        }
    }

    Test fun simpleCount() {
        assertEquals(2, data.count())
        assertEquals(3, hashSet(12, 14, 15).count())
        assertEquals(0, ArrayList<Double>().count())
    }

    Test fun last() {
        assertEquals("bar", data.last())
        assertEquals(25, arrayList(15, 19, 20, 25).last())
        // assertEquals(19, TreeSet(arrayList(90, 47, 19)).first())
        assertEquals('a', linkedList('a').last())
    }

    Test fun lastException() {
        fails { arrayList<Int>().last() }
        fails { linkedList<String>().last() }
        fails { hashSet<Char>().last() }
    }

    Test fun subscript() {
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

    Test fun indices() {
        val indices = data.indices
        assertEquals(0, indices.start)
        assertEquals(1, indices.end)
        assertEquals(2, indices.size)
        assertFalse(indices.isReversed)
    }

    Test fun contains() {
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
}
