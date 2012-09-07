package test.collections

import kotlin.test.*

import java.util.*

import org.junit.Test as test

class CollectionJVMTest {

    test fun flatMap() {
        val data = arrayList("", "foo", "bar", "x", "")
        val characters = data.flatMap<String,Char>{ it.toCharList() }
        println("Got list of characters ${characters}")
        assertEquals(7, characters.size())
        val text = characters.makeString("")
        assertEquals("foobarx", text)
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


    // TODO would be nice to avoid the <String>
    test fun filterIntoSortedSet() {
        val data = arrayList("foo", "bar")
        val sorted = data.filterTo(sortedSet<String>()){it.length == 3}
        assertEquals(2, sorted.size)
        assertEquals(sortedSet("bar", "foo"), sorted)
        assertTrue {
            sorted is TreeSet<String>
        }
    }

        //todo after KT-1873 the name might be returned to 'last'
    test fun lastElement() {
        val data = arrayList("foo", "bar")
        assertEquals("bar", data.last())
        assertEquals(25, arrayList(15, 19, 20, 25).last())
        assertEquals('a', linkedList('a').last())
    }

    test fun lastException() {
        fails { linkedList<String>().last() }
    }

    test fun contains() {
        assertTrue(linkedList(15, 19, 20).contains(15))
    }

    test fun sortBy() {
        expect(arrayList("two" to 2, "three" to 3)) {
            arrayList("three" to 3, "two" to 2).sortBy { it.second }
        }
        expect(arrayList("three" to 3, "two" to 2)) {
            arrayList("three" to 3, "two" to 2).sortBy { it.first }
        }
        expect(arrayList("two" to 2, "three" to 3)) {
            arrayList("three" to 3, "two" to 2).sortBy { it.first.length }
        }
    }


    test fun sortFunctionShouldReturnSortedCopyForList() {
        // TODO fixme Some sort of in/out variance thing - or an issue with Java interop?
        todo {
//            val list : List<Int> = arrayList<Int>(2, 3, 1)
//            expect(arrayList(1, 2, 3)) { list.sort() }
//            expect(arrayList(2, 3, 1)) { list }
        }
    }

    test fun sortFunctionShouldReturnSortedCopyForIterable() {
        // TODO fixme Some sort of in/out variance thing - or an issue with Java interop?
        todo {
//        val list : Iterable<Int> = arrayList(2, 3, 1)
//        expect(arrayList(1, 2, 3)) { list.sort() }
//        expect(arrayList(2, 3, 1)) { list }
        }
    }
}
