package test.collections

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.*
import kotlin.comparisons.*
import java.util.*

import org.junit.Test as test

class CollectionJVMTest {

    private fun <T> identitySetOf(vararg values: T): MutableSet<T> {
        val map = IdentityHashMap<T, String>()
        values.forEach { map.put(it, "") }
        return map.keys
    }

    private data class IdentityData(public val value: Int)

    @test fun removeAllWithDifferentEquality() {
        val data = listOf(IdentityData(1), IdentityData(1))
        val list = data.toMutableList()
        list -= identitySetOf(data[0]) as Iterable<IdentityData>
        assertTrue(list.single() === data[1], "Identity contains should be used")

        val list2 = data.toMutableList()
        list2 -= hashSetOf(data[0]) as Iterable<IdentityData>
        assertTrue(list2.isEmpty(), "Equality contains should be used")

        val set3: MutableSet<IdentityData> = identitySetOf(*data.toTypedArray())
        set3 -= arrayOf(data[1])
        assertTrue(set3.isEmpty(), "Array doesn't have contains, equality contains is used instead")
    }

    @test fun flatMap() {
        val data = listOf("", "foo", "bar", "x", "")
        val characters = data.flatMap { it.toList() }
        println("Got list of characters ${characters}")
        assertEquals(7, characters.size)
        val text = characters.joinToString("")
        assertEquals("foobarx", text)
    }


    @test fun filterIntoLinkedList() {
        val data = listOf("foo", "bar")
        val foo = data.filterTo(LinkedList<String>()) { it.startsWith("f") }

        assertTrue {
            foo.all { it.startsWith("f") }
        }
        assertEquals(1, foo.size)
        assertEquals(listOf("foo"), foo)

        assertTrue {
            foo is LinkedList<String>
        }
    }

    @test fun filterNotIntoLinkedListOf() {
        val data = listOf("foo", "bar")
        val foo = data.filterNotTo(LinkedList<String>()) { it.startsWith("f") }

        assertTrue {
            foo.all { !it.startsWith("f") }
        }
        assertEquals(1, foo.size)
        assertEquals(listOf("bar"), foo)

        assertTrue {
            foo is LinkedList<String>
        }
    }

    @test fun filterNotNullIntoLinkedListOf() {
        val data = listOf(null, "foo", null, "bar")
        val foo = data.filterNotNullTo(LinkedList<String>())

        assertEquals(2, foo.size)
        assertEquals(LinkedList(listOf("foo", "bar")), foo)

        assertTrue {
            foo is LinkedList<String>
        }
    }

    @test fun filterIntoSortedSet() {
        val data = listOf("foo", "bar")
        val sorted = data.filterTo(sortedSetOf<String>()) { it.length == 3 }
        assertEquals(2, sorted.size)
        assertEquals(sortedSetOf("bar", "foo"), sorted)
        assertTrue {
            sorted is TreeSet<String>
        }
    }

    @test fun first() {
        assertEquals(19, TreeSet(listOf(90, 47, 19)).first())
    }

    @test fun last() {
        val data = listOf("foo", "bar")
        assertEquals("bar", data.last())
        assertEquals(25, listOf(15, 19, 20, 25).last())
        assertEquals('a', LinkedList(listOf('a')).last())
    }

    @test fun lastException() {
        assertFails { LinkedList<String>().last() }
    }

    @test fun contains() {
        assertTrue(LinkedList(listOf(15, 19, 20)).contains(15))
    }

    @test fun toArray() {
        val data = listOf("foo", "bar")
        val arr = data.toTypedArray()
        println("Got array ${arr}")
        assertEquals(2, arr.size)
        todo {
            assertTrue {
                arr.isArrayOf<String>()
            }
        }
    }

    @test fun toSortedSet() {
        val data = listOf("foo", "Foo", "bar")
        val set1 = data.toSortedSet()
        assertEquals(listOf("Foo", "bar", "foo"), set1.toList())

        val set2 = data.toSortedSet(reverseOrder())
        assertEquals(listOf("foo", "bar", "Foo"), set2.toList())

        val set3 = data.toSortedSet(String.CASE_INSENSITIVE_ORDER)
        assertEquals(listOf("bar", "foo"), set3.toList())
    }

    @test fun takeReturnsFirstNElements() {
        expect(setOf(1, 2)) { sortedSetOf(1, 2, 3, 4, 5).take(2).toSet() }
    }

    @test fun filterIsInstanceList() {
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
        assertEquals(0, charValues.size)
    }

    @test fun filterIsInstanceArray() {
        val src: Array<Any> = arrayOf(1, 2, 3.toDouble(), "abc", "cde")

        val intValues: List<Int> = src.filterIsInstance<Int>()
        assertEquals(listOf(1, 2), intValues)

        val doubleValues: List<Double> = src.filterIsInstance<Double>()
        assertEquals(listOf(3.0), doubleValues)

        val stringValues: List<String> = src.filterIsInstance<String>()
        assertEquals(listOf("abc", "cde"), stringValues)

        val anyValues: List<Any> = src.filterIsInstance<Any>()
        assertEquals(src.toList(), anyValues)

        val charValues: List<Char> = src.filterIsInstance<Char>()
        assertEquals(0, charValues.size)
    }

    @test fun emptyListIsSerializable() = testSingletonSerialization(emptyList<Any>())

    @test fun emptySetIsSerializable() = testSingletonSerialization(emptySet<Any>())

    @test fun emptyMapIsSerializable() = testSingletonSerialization(emptyMap<Any, Any>())

    private fun testSingletonSerialization(value: Any) {
        val result = serializeAndDeserialize(value)

        assertEquals(value, result)
        assertTrue(value === result)
    }

    private fun <T> serializeAndDeserialize(value: T): T {
        val outputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(outputStream)

        objectOutputStream.writeObject(value)
        objectOutputStream.close()
        outputStream.close()

        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val inputObjectStream = ObjectInputStream(inputStream)
        return inputObjectStream.readObject() as T
    }

    @test fun deserializeEmptyList() = testPersistedDeserialization(
            "ac ed 00 05 73 72 00 1c 6b 6f 74 6c 69 6e 2e 63 6f 6c 6c 65 63 74 69 6f 6e 73 2e 45 6d 70 74 79 4c 69 73 74 99 6f c7 d0 a7 e0 60 32 02 00 00 78 70",
            emptyList<Any>())

    @test fun deserializeEmptySet() = testPersistedDeserialization(
            "ac ed 00 05 73 72 00 1b 6b 6f 74 6c 69 6e 2e 63 6f 6c 6c 65 63 74 69 6f 6e 73 2e 45 6d 70 74 79 53 65 74 2f 46 b0 15 76 d7 e2 f4 02 00 00 78 70",
            emptySet<Any>())

    @test fun deserializeEmptyMap() = testPersistedDeserialization(
            "ac ed 00 05 73 72 00 1b 6b 6f 74 6c 69 6e 2e 63 6f 6c 6c 65 63 74 69 6f 6e 73 2e 45 6d 70 74 79 4d 61 70 72 72 37 71 cb 04 4c d2 02 00 00 78 70",
            emptyMap<Any, Any>())

    private fun testPersistedDeserialization(hexValue: String, expected: Any) {
        val actual = deserializeFromHex<Any>(hexValue)
        assertEquals(expected, actual)
    }

    private fun hexToBytes(value: String): ByteArray = value.split(" ").map { Integer.parseInt(it, 16).toByte() }.toByteArray()

    private fun <T> deserializeFromHex(value: String) = hexToBytes(value).let {
        val inputStream = ByteArrayInputStream(it)
        val inputObjectStream = ObjectInputStream(inputStream)
        inputObjectStream.readObject() as T
    }
}


public fun assertTypeEquals(expected: Any?, actual: Any?) {
    assertEquals(expected?.javaClass, actual?.javaClass)
}
