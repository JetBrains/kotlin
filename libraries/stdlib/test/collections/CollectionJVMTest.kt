package test.collections

import kotlin.test.*

import java.util.*

import org.junit.Test as test

class CollectionJVMTest {

    test fun flatMap() {
        val data = listOf("", "foo", "bar", "x", "")
        val characters = data.flatMap { it.toCharList() }
        println("Got list of characters ${characters}")
        assertEquals(7, characters.size())
        val text = characters.joinToString("")
        assertEquals("foobarx", text)
    }


    test fun filterIntoLinkedList() {
        val data = listOf("foo", "bar")
        val foo = data.filterTo(linkedListOf<String>()) { it.startsWith("f") }

        assertTrue {
            foo.all { it.startsWith("f") }
        }
        assertEquals(1, foo.size())
        assertEquals(linkedListOf("foo"), foo)

        assertTrue {
            foo is LinkedList<String>
        }
    }

    test fun filterNotIntoLinkedListOf() {
        val data = listOf("foo", "bar")
        val foo = data.filterNotTo(linkedListOf<String>()) { it.startsWith("f") }

        assertTrue {
            foo.all { !it.startsWith("f") }
        }
        assertEquals(1, foo.size())
        assertEquals(linkedListOf("bar"), foo)

        assertTrue {
            foo is LinkedList<String>
        }
    }

    test fun filterNotNullIntoLinkedListOf() {
        val data = listOf(null, "foo", null, "bar")
        val foo = data.filterNotNullTo(linkedListOf<String>())

        assertEquals(2, foo.size())
        assertEquals(linkedListOf("foo", "bar"), foo)

        assertTrue {
            foo is LinkedList<String>
        }
    }

    test fun filterIntoSortedSet() {
        val data = listOf("foo", "bar")
        val sorted = data.filterTo(sortedSetOf<String>()) { it.length() == 3 }
        assertEquals(2, sorted.size())
        assertEquals(sortedSetOf("bar", "foo"), sorted)
        assertTrue {
            sorted is TreeSet<String>
        }
    }

    test fun first() {
        assertEquals(19, TreeSet(listOf(90, 47, 19)).first())
    }

    test fun last() {
        val data = listOf("foo", "bar")
        assertEquals("bar", data.last())
        assertEquals(25, listOf(15, 19, 20, 25).last())
        assertEquals('a', linkedListOf('a').last())
    }

    test fun lastException() {
        fails { linkedListOf<String>().last() }
    }

    test fun contains() {
        assertTrue(linkedListOf(15, 19, 20).contains(15))
    }

    test fun toArray() {
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

    test fun takeReturnsFirstNElements() {
        expect(setOf(1, 2)) { sortedSetOf(1, 2, 3, 4, 5).take(2).toSet() }
    }

    test fun filterIsInstanceList() {
        val values: List<Any> = listOf(1, 2, 3.toDouble(), "abc", "cde")

        val intValues: List<Int> = values.filterIsInstance<Int>()
        assertEquals(listOf(1, 2), intValues)

        val doubleValues: List<Double> = values.filterIsInstance<Double>()
        assertEquals(listOf(3.0), doubleValues)

        val stringValues: List<String> = values.filterIsInstance<String>()
        assertEquals(listOf("abc", "cde"), stringValues)

        val anyValues: List<Any> = values.filterIsInstance<Any>()
        assertEquals(values.toList(), anyValues)

        val charValues: List<Char> = values.filterIsInstance<Char>()
        assertEquals(0, charValues.size())
    }

    test fun filterIsInstanceArray() {
        val src: Array<Any> = array(1, 2, 3.toDouble(), "abc", "cde")

        val intValues: List<Int> = src.filterIsInstance<Int>()
        assertEquals(listOf(1, 2), intValues)

        val doubleValues: List<Double> = src.filterIsInstance<Double>()
        assertEquals(listOf(3.0), doubleValues)

        val stringValues: List<String> = src.filterIsInstance<String>()
        assertEquals(listOf("abc", "cde"), stringValues)

        val anyValues: List<Any> = src.filterIsInstance<Any>()
        assertEquals(src.toList(), anyValues)

        val charValues: List<Char> = src.filterIsInstance<Char>()
        assertEquals(0, charValues.size())
    }
}
