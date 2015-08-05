package test.collections

import kotlin.test.*
import org.junit.Test as test

fun <T> assertArrayNotSameButEquals(expected: Array<out T>, actual: Array<out T>, message: String = "") { assertTrue(expected !== actual, message); assertEquals(expected.toList(), actual.toList(), message) }
fun assertArrayNotSameButEquals(expected: IntArray, actual: IntArray, message: String = "") { assertTrue(expected !== actual, message); assertEquals(expected.toList(), actual.toList(), message) }
fun assertArrayNotSameButEquals(expected: LongArray, actual: LongArray, message: String = "") { assertTrue(expected !== actual, message); assertEquals(expected.toList(), actual.toList(), message) }
fun assertArrayNotSameButEquals(expected: ShortArray, actual: ShortArray, message: String = "") { assertTrue(expected !== actual, message); assertEquals(expected.toList(), actual.toList(), message) }
fun assertArrayNotSameButEquals(expected: ByteArray, actual: ByteArray, message: String = "") { assertTrue(expected !== actual, message); assertEquals(expected.toList(), actual.toList(), message) }
fun assertArrayNotSameButEquals(expected: DoubleArray, actual: DoubleArray, message: String = "") { assertTrue(expected !== actual, message); assertEquals(expected.toList(), actual.toList(), message) }
fun assertArrayNotSameButEquals(expected: FloatArray, actual: FloatArray, message: String = "") { assertTrue(expected !== actual, message); assertEquals(expected.toList(), actual.toList(), message) }
fun assertArrayNotSameButEquals(expected: CharArray, actual: CharArray, message: String = "") { assertTrue(expected !== actual, message); assertEquals(expected.toList(), actual.toList(), message) }
fun assertArrayNotSameButEquals(expected: BooleanArray, actual: BooleanArray, message: String = "") { assertTrue(expected !== actual, message); assertEquals(expected.toList(), actual.toList(), message) }


class ArraysTest {

    test fun emptyArrayLastIndex() {
        val arr1 = IntArray(0)
        assertEquals(-1, arr1.lastIndex)

        val arr2 = emptyArray<String>()
        assertEquals(-1, arr2.lastIndex)
    }

    test fun arrayLastIndex() {
        val arr1 = intArrayOf(0, 1, 2, 3, 4)
        assertEquals(4, arr1.lastIndex)
        assertEquals(4, arr1[arr1.lastIndex])

        val arr2 = Array<String>(5, { "$it" })
        assertEquals(4, arr2.lastIndex)
        assertEquals("4", arr2[arr2.lastIndex])
    }

    test fun byteArray() {
        val arr = ByteArray(2)

        val expected: Byte = 0
        assertEquals(arr.size(), 2)
        assertEquals(expected, arr[0])
        assertEquals(expected, arr[1])
    }

    test fun shortArray() {
        val arr = ShortArray(2)

        val expected: Short = 0
        assertEquals(arr.size(), 2)
        assertEquals(expected, arr[0])
        assertEquals(expected, arr[1])
    }

    test fun intArray() {
        val arr = IntArray(2)

        assertEquals(arr.size(), 2)
        assertEquals(0, arr[0])
        assertEquals(0, arr[1])
    }

    test fun longArray() {
        val arr = LongArray(2)

        val expected: Long = 0
        assertEquals(arr.size(), 2)
        assertEquals(expected, arr[0])
        assertEquals(expected, arr[1])
    }

    test fun floatArray() {
        val arr = FloatArray(2)

        val expected: Float = 0.0F
        assertEquals(arr.size(), 2)
        assertEquals(expected, arr[0])
        assertEquals(expected, arr[1])
    }

    test fun doubleArray() {
        val arr = DoubleArray(2)

        assertEquals(arr.size(), 2)
        assertEquals(0.0, arr[0])
        assertEquals(0.0, arr[1])
    }

    test fun charArray() {
        val arr = CharArray(2)

        val expected: Char = '\u0000'
        assertEquals(arr.size(), 2)
        assertEquals(expected, arr[0])
        assertEquals(expected, arr[1])
    }

    test fun booleanArray() {
        val arr = BooleanArray(2)
        assertEquals(arr.size(), 2)
        assertEquals(false, arr[0])
        assertEquals(false, arr[1])
    }

    test fun min() {
        expect(null, { arrayOf<Int>().min() })
        expect(1, { arrayOf(1).min() })
        expect(2, { arrayOf(2, 3).min() })
        expect(2000000000000, { arrayOf(3000000000000, 2000000000000).min() })
        expect('a', { arrayOf('a', 'b').min() })
        expect("a", { arrayOf("a", "b").min() })
    }

    test fun minInPrimitiveArrays() {
        expect(null, { intArrayOf().min() })
        expect(1, { intArrayOf(1).min() })
        expect(2, { intArrayOf(2, 3).min() })
        expect(2000000000000, { longArrayOf(3000000000000, 2000000000000).min() })
        expect(1, { byteArrayOf(1, 3, 2).min() })
        expect(2, { shortArrayOf(3, 2).min() })
        expect(2.0F, { floatArrayOf(3.0F, 2.0F).min() })
        expect(2.0, { doubleArrayOf(2.0, 3.0).min() })
        expect('a', { charArrayOf('a', 'b').min() })
    }

    test fun max() {
        expect(null, { arrayOf<Int>().max() })
        expect(1, { arrayOf(1).max() })
        expect(3, { arrayOf(2, 3).max() })
        expect(3000000000000, { arrayOf(3000000000000, 2000000000000).max() })
        expect('b', { arrayOf('a', 'b').max() })
        expect("b", { arrayOf("a", "b").max() })
    }

    test fun maxInPrimitiveArrays() {
        expect(null, { intArrayOf().max() })
        expect(1, { intArrayOf(1).max() })
        expect(3, { intArrayOf(2, 3).max() })
        expect(3000000000000, { longArrayOf(3000000000000, 2000000000000).max() })
        expect(3, { byteArrayOf(1, 3, 2).max() })
        expect(3, { shortArrayOf(3, 2).max() })
        expect(3.0F, { floatArrayOf(3.0F, 2.0F).max() })
        expect(3.0, { doubleArrayOf(2.0, 3.0).max() })
        expect('b', { charArrayOf('a', 'b').max() })
    }

    test fun minBy() {
        expect(null, { arrayOf<Int>().minBy { it } })
        expect(1, { arrayOf(1).minBy { it } })
        expect(3, { arrayOf(2, 3).minBy { -it } })
        expect('a', { arrayOf('a', 'b').minBy { "x$it" } })
        expect("b", { arrayOf("b", "abc").minBy { it.length() } })
    }

    test fun minByInPrimitiveArrays() {
        expect(null, { intArrayOf().minBy { it } })
        expect(1, { intArrayOf(1).minBy { it } })
        expect(3, { intArrayOf(2, 3).minBy { -it } })
        expect(2000000000000, { longArrayOf(3000000000000, 2000000000000).minBy { it + 1 } })
        expect(1, { byteArrayOf(1, 3, 2).minBy { it * it } })
        expect(3, { shortArrayOf(3, 2).minBy { "a" } })
        expect(2.0F, { floatArrayOf(3.0F, 2.0F).minBy { it.toString() } })
        expect(2.0, { doubleArrayOf(2.0, 3.0).minBy { Math.sqrt(it) } })
    }

    test fun maxBy() {
        expect(null, { arrayOf<Int>().maxBy { it } })
        expect(1, { arrayOf(1).maxBy { it } })
        expect(2, { arrayOf(2, 3).maxBy { -it } })
        expect('b', { arrayOf('a', 'b').maxBy { "x$it" } })
        expect("abc", { arrayOf("b", "abc").maxBy { it.length() } })
    }

    test fun maxByInPrimitiveArrays() {
        expect(null, { intArrayOf().maxBy { it } })
        expect(1, { intArrayOf(1).maxBy { it } })
        expect(2, { intArrayOf(2, 3).maxBy { -it } })
        expect(3000000000000, { longArrayOf(3000000000000, 2000000000000).maxBy { it + 1 } })
        expect(3, { byteArrayOf(1, 3, 2).maxBy { it * it } })
        expect(3, { shortArrayOf(3, 2).maxBy { "a" } })
        expect(3.0F, { floatArrayOf(3.0F, 2.0F).maxBy { it.toString() } })
        expect(3.0, { doubleArrayOf(2.0, 3.0).maxBy { Math.sqrt(it) } })
    }

    test fun minIndex() {
        val a = intArrayOf(1, 7, 9, -42, 54, 93)
        expect(3, { a.indices.minBy { a[it] } })
    }

    test fun maxIndex() {
        val a = intArrayOf(1, 7, 9, 239, 54, 93)
        expect(3, { a.indices.maxBy { a[it] } })
    }

    test fun minByEvaluateOnce() {
        var c = 0
        expect(1, { arrayOf(5, 4, 3, 2, 1).minBy { c++; it * it } })
        assertEquals(5, c)
    }

    test fun maxByEvaluateOnce() {
        var c = 0
        expect(5, { arrayOf(5, 4, 3, 2, 1).maxBy { c++; it * it } })
        assertEquals(5, c)
    }

    test fun sum() {
        expect(0) { arrayOf<Int>().sum() }
        expect(14) { arrayOf(2, 3, 9).sum() }
        expect(3.0) { arrayOf(1.0, 2.0).sum() }
        expect(200) { arrayOf<Byte>(100, 100).sum() }
        expect(50000) { arrayOf<Short>(20000, 30000).sum() }
        expect(3000000000000) { arrayOf<Long>(1000000000000, 2000000000000).sum() }
        expect(3.0F) { arrayOf<Float>(1.0F, 2.0F).sum() }
    }

    test fun sumInPrimitiveArrays() {
        expect(0) { intArrayOf().sum() }
        expect(14) { intArrayOf(2, 3, 9).sum() }
        expect(3.0) { doubleArrayOf(1.0, 2.0).sum() }
        expect(200) { byteArrayOf(100, 100).sum() }
        expect(50000) { shortArrayOf(20000, 30000).sum() }
        expect(3000000000000) { longArrayOf(1000000000000, 2000000000000).sum() }
        expect(3.0F) { floatArrayOf(1.0F, 2.0F).sum() }
    }

    test fun average() {
        expect(0.0) { arrayOf<Int>().average() }
        expect(3.8) { arrayOf(1, 2, 5, 8, 3).average() }
        expect(2.1) { arrayOf(1.6, 2.6, 3.6, 0.6).average() }
        expect(100.0) { arrayOf<Byte>(100, 100, 100, 100, 100, 100).average() }
        expect(0) { arrayOf<Short>(1, -1, 2, -2, 3, -3).average().toShort() }
        // TODO: Property based tests
        // for each arr with size 1 arr.average() == arr[0]
        // for each arr with size > 0  arr.average() = arr.sum().toDouble() / arr.size()
    }

    test fun indexOfInPrimitiveArrays() {
        expect(-1) { byteArrayOf(1, 2, 3) indexOf 0 }
        expect(0) { byteArrayOf(1, 2, 3) indexOf 1 }
        expect(1) { byteArrayOf(1, 2, 3) indexOf 2 }
        expect(2) { byteArrayOf(1, 2, 3) indexOf 3 }

        expect(-1) { shortArrayOf(1, 2, 3) indexOf 0 }
        expect(0) { shortArrayOf(1, 2, 3) indexOf 1 }
        expect(1) { shortArrayOf(1, 2, 3) indexOf 2 }
        expect(2) { shortArrayOf(1, 2, 3) indexOf 3 }

        expect(-1) { intArrayOf(1, 2, 3) indexOf 0 }
        expect(0) { intArrayOf(1, 2, 3) indexOf 1 }
        expect(1) { intArrayOf(1, 2, 3) indexOf 2 }
        expect(2) { intArrayOf(1, 2, 3) indexOf 3 }

        expect(-1) { longArrayOf(1, 2, 3) indexOf 0 }
        expect(0) { longArrayOf(1, 2, 3) indexOf 1 }
        expect(1) { longArrayOf(1, 2, 3) indexOf 2 }
        expect(2) { longArrayOf(1, 2, 3) indexOf 3 }

        expect(-1) { floatArrayOf(1.0f, 2.0f, 3.0f) indexOf 0f }
        expect(0) { floatArrayOf(1.0f, 2.0f, 3.0f) indexOf 1.0f }
        expect(1) { floatArrayOf(1.0f, 2.0f, 3.0f) indexOf 2.0f }
        expect(2) { floatArrayOf(1.0f, 2.0f, 3.0f) indexOf 3.0f }

        expect(-1) { doubleArrayOf(1.0, 2.0, 3.0) indexOf 0.0 }
        expect(0) { doubleArrayOf(1.0, 2.0, 3.0) indexOf 1.0 }
        expect(1) { doubleArrayOf(1.0, 2.0, 3.0) indexOf 2.0 }
        expect(2) { doubleArrayOf(1.0, 2.0, 3.0) indexOf 3.0 }

        expect(-1) { charArrayOf('a', 'b', 'c') indexOf 'z' }
        expect(0) { charArrayOf('a', 'b', 'c') indexOf 'a' }
        expect(1) { charArrayOf('a', 'b', 'c') indexOf 'b' }
        expect(2) { charArrayOf('a', 'b', 'c') indexOf 'c' }

        expect(0) { booleanArrayOf(true, false) indexOf true }
        expect(1) { booleanArrayOf(true, false) indexOf false }
        expect(-1) { booleanArrayOf(true) indexOf false }
    }

    test fun indexOf() {
        expect(-1) { arrayOf("cat", "dog", "bird").indexOf("mouse") }
        expect(0) { arrayOf("cat", "dog", "bird").indexOf("cat") }
        expect(1) { arrayOf("cat", "dog", "bird").indexOf("dog") }
        expect(2) { arrayOf("cat", "dog", "bird").indexOf("bird") }
        expect(0) { arrayOf(null, "dog", null).indexOf(null : String?)}

        expect(-1) { arrayOf("cat", "dog", "bird").indexOfFirst { it.contains("p") } }
        expect(0) { arrayOf("cat", "dog", "bird").indexOfFirst { it.startsWith('c') } }
        expect(1) { arrayOf("cat", "dog", "bird").indexOfFirst { it.startsWith('d') } }
        expect(2) { arrayOf("cat", "dog", "bird").indexOfFirst { it.endsWith('d') } }

        expect(-1) { sequenceOf("cat", "dog", "bird").indexOfFirst { it.contains("p") } }
        expect(0) { sequenceOf("cat", "dog", "bird").indexOfFirst { it.startsWith('c') } }
        expect(1) { sequenceOf("cat", "dog", "bird").indexOfFirst { it.startsWith('d') } }
        expect(2) { sequenceOf("cat", "dog", "bird").indexOfFirst { it.endsWith('d') } }
    }

    test fun lastIndexOf() {
        expect(-1) { arrayOf("cat", "dog", "bird").lastIndexOf("mouse") }
        expect(0) { arrayOf("cat", "dog", "bird").lastIndexOf("cat") }
        expect(1) { arrayOf("cat", "dog", "bird").lastIndexOf("dog") }
        expect(2) { arrayOf(null, "dog", null).lastIndexOf(null : String?)}
        expect(3) { arrayOf("cat", "dog", "bird", "dog").lastIndexOf("dog") }

        expect(-1) { arrayOf("cat", "dog", "bird").indexOfLast { it.contains("p") } }
        expect(0) { arrayOf("cat", "dog", "bird").indexOfLast { it.startsWith('c') } }
        expect(2) { arrayOf("cat", "dog", "cap", "bird").indexOfLast { it.startsWith('c') } }
        expect(2) { arrayOf("cat", "dog", "bird").indexOfLast { it.endsWith('d') } }
        expect(3) { arrayOf("cat", "dog", "bird", "red").indexOfLast { it.endsWith('d') } }

        expect(-1) { sequenceOf("cat", "dog", "bird").indexOfLast { it.contains("p") } }
        expect(0) { sequenceOf("cat", "dog", "bird").indexOfLast { it.startsWith('c') } }
        expect(2) { sequenceOf("cat", "dog", "cap", "bird").indexOfLast { it.startsWith('c') } }
        expect(2) { sequenceOf("cat", "dog", "bird").indexOfLast { it.endsWith('d') } }
        expect(3) { sequenceOf("cat", "dog", "bird", "red").indexOfLast { it.endsWith('d') } }
    }

    test fun isEmpty() {
        assertTrue(emptyArray<String>().isEmpty())
        assertFalse(arrayOf("").isEmpty())
        assertTrue(intArrayOf().isEmpty())
        assertFalse(intArrayOf(1).isEmpty())
        assertTrue(byteArrayOf().isEmpty())
        assertFalse(byteArrayOf(1).isEmpty())
        assertTrue(shortArrayOf().isEmpty())
        assertFalse(shortArrayOf(1).isEmpty())
        assertTrue(longArrayOf().isEmpty())
        assertFalse(longArrayOf(1).isEmpty())
        assertTrue(charArrayOf().isEmpty())
        assertFalse(charArrayOf('a').isEmpty())
        assertTrue(floatArrayOf().isEmpty())
        assertFalse(floatArrayOf(0.1F).isEmpty())
        assertTrue(doubleArrayOf().isEmpty())
        assertFalse(doubleArrayOf(0.1).isEmpty())
        assertTrue(booleanArrayOf().isEmpty())
        assertFalse(booleanArrayOf(false).isEmpty())
    }

    test fun isNotEmpty() {
        assertFalse(intArrayOf().isNotEmpty())
        assertTrue(intArrayOf(1).isNotEmpty())
    }

    test fun plus() {
        assertEquals(listOf("1", "2", "3", "4"), listOf("1", "2") + arrayOf("3", "4"))
        assertArrayNotSameButEquals(arrayOf("1", "2", "3"), arrayOf("1", "2") + "3")
        assertArrayNotSameButEquals(arrayOf("1", "2", "3", "4"), arrayOf("1", "2") + arrayOf("3", "4"))
        assertArrayNotSameButEquals(arrayOf("1", "2", "3", "4"), arrayOf("1", "2") + listOf("3", "4"))
        assertArrayNotSameButEquals(intArrayOf(1, 2, 3), intArrayOf(1, 2) + 3)
        assertArrayNotSameButEquals(intArrayOf(1, 2, 3, 4), intArrayOf(1, 2) + listOf(3, 4))
        assertArrayNotSameButEquals(intArrayOf(1, 2, 3, 4), intArrayOf(1, 2) + intArrayOf(3, 4))
    }

    test fun plusVararg() {
        fun stringOnePlus(vararg a: String) = arrayOf("1") + a
        fun longOnePlus(vararg a: Long) = longArrayOf(1) + a
        fun intOnePlus(vararg a: Int) = intArrayOf(1) + a

        assertArrayNotSameButEquals(arrayOf("1", "2"), stringOnePlus("2"), "Array.plus")
        assertArrayNotSameButEquals(intArrayOf(1, 2), intOnePlus(2), "IntArray.plus")
        assertArrayNotSameButEquals(longArrayOf(1, 2), longOnePlus(2), "LongArray.plus")
    }

    test fun plusAssign() {
        // lets use a mutable variable
        var result = arrayOf("a")
        result += "foo"
        result += listOf("beer")
        result += arrayOf("cheese", "wine")
        assertArrayNotSameButEquals(arrayOf("a", "foo", "beer", "cheese", "wine"), result)
    }

    test fun first() {
        expect(1) { arrayOf(1, 2, 3).first() }
        expect(2) { arrayOf(1, 2, 3).first { it % 2 == 0 } }
    }

    test fun last() {
        expect(3) { arrayOf(1, 2, 3).last() }
        expect(2) { arrayOf(1, 2, 3).last { it % 2 == 0 } }
    }

    test fun contains() {
        assertTrue(arrayOf("1", "2", "3", "4").contains("2"))
        assertTrue("3" in arrayOf("1", "2", "3", "4"))
        assertTrue("0" !in arrayOf("1", "2", "3", "4"))
    }

    test fun slice() {
        val iter = listOf(3, 1, 2)

        assertEquals(listOf("B"), arrayOf("A", "B", "C").slice(1..1))
        assertEquals(listOf('E', 'B', 'C'), arrayOf('A', 'B', 'C', 'E').slice(iter))

        assertEquals(listOf<Int>(), arrayOf<Int>().slice(5..4))
        assertEquals(listOf<Int>(), arrayOf(1, 2, 3).slice(5..1))
        assertEquals(listOf(2, 3, 9), arrayOf(2, 3, 9, 2, 3, 9).slice(iter))
        assertEquals(listOf(2.0, 3.0), arrayOf(2.0, 3.0, 9.0).slice(0..1))
        assertEquals(listOf(2f, 3f), arrayOf(2f, 3f, 9f).slice(0..1))
        assertEquals(listOf<Byte>(127, 100), arrayOf<Byte>(50, 100, 127).slice(2 downTo 1))
        assertEquals(listOf<Short>(200, 100), arrayOf<Short>(50, 100, 200).slice(2 downTo 1))
        assertEquals(listOf(100L, 200L, 30L), arrayOf(50L, 100L, 200L, 30L).slice(1..3))
        assertEquals(listOf(true, false, true), arrayOf(true, false, true, true).slice(iter))
    }

    test fun asIterable() {
        val arr1 = intArrayOf(1, 2, 3, 4, 5)
        val iter1 = arr1.asIterable()
        assertEquals(arr1.toList(), iter1.toList())
        arr1[0] = 0
        assertEquals(arr1.toList(), iter1.toList())

        val arr2 = arrayOf("one", "two", "three")
        val iter2 = arr2.asIterable()
        assertEquals(arr2.toList(), iter2.toList())
        arr2[0] = ""
        assertEquals(arr2.toList(), iter2.toList())

        val arr3 = IntArray(0)
        val iter3 = arr3.asIterable()
        assertEquals(iter3.toList(), emptyList<Int>())

        val arr4 = Array(0, { "$it" })
        val iter4 = arr4.asIterable()
        assertEquals(iter4.toList(), emptyList<String>())
    }

    test fun asList() {
        assertEquals(listOf(1, 2, 3), intArrayOf(1, 2, 3).asList())
        assertEquals(listOf<Byte>(1, 2, 3), byteArrayOf(1, 2, 3).asList())
        assertEquals(listOf(true, false), booleanArrayOf(true, false).asList())

        assertEquals(listOf(1, 2, 3), arrayOf(1, 2, 3).asList())
        assertEquals(listOf("abc", "def"), arrayOf("abc", "def").asList())

        val ints = intArrayOf(1, 5, 7)
        val intsAsList = ints.asList()
        assertEquals(5, intsAsList[1])
        ints[1] = 10
        assertEquals(10, intsAsList[1], "Should reflect changes in original array")
    }

    test fun toPrimitiveArray() {
        val genericArray: Array<Int> = arrayOf(1, 2, 3)
        val primitiveArray: IntArray = genericArray.toIntArray()
        expect(3) { primitiveArray.size() }
        assertEquals(genericArray.asList(), primitiveArray.asList())


        val charList = listOf('a', 'b')
        val charArray: CharArray = charList.toCharArray()
        assertEquals(charList, charArray.asList())
    }

    test fun toTypedArray() {
        val primitiveArray: LongArray = longArrayOf(1, 2, Long.MAX_VALUE)
        val genericArray: Array<Long> = primitiveArray.toTypedArray()
        expect(3) { genericArray.size() }
        assertEquals(primitiveArray.asList(), genericArray.asList())
    }

    test fun copyOf() {
        booleanArrayOf(true, false, true).let { assertArrayNotSameButEquals(it, it.copyOf()) }
        byteArrayOf(0, 1, 2, 3, 4, 5).let { assertArrayNotSameButEquals(it, it.copyOf()) }
        shortArrayOf(0, 1, 2, 3, 4, 5).let { assertArrayNotSameButEquals(it, it.copyOf()) }
        intArrayOf(0, 1, 2, 3, 4, 5).let { assertArrayNotSameButEquals(it, it.copyOf()) }
        longArrayOf(0, 1, 2, 3, 4, 5).let { assertArrayNotSameButEquals(it, it.copyOf()) }
        floatArrayOf(0F, 1F, 2F, 3F).let { assertArrayNotSameButEquals(it, it.copyOf()) }
        doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0).let { assertArrayNotSameButEquals(it, it.copyOf()) }
        charArrayOf('0', '1', '2', '3', '4', '5').let { assertArrayNotSameButEquals(it, it.copyOf()) }
    }

    test fun copyAndResize() {
        assertArrayNotSameButEquals(arrayOf("1", "2"), arrayOf("1", "2", "3").copyOf(2))
        assertArrayNotSameButEquals(arrayOf("1", "2", null), arrayOf("1", "2").copyOf(3))

        assertArrayNotSameButEquals(longArrayOf(1, 2), longArrayOf(1, 2, 3).copyOf(2))
        assertArrayNotSameButEquals(longArrayOf(1, 2, 0), longArrayOf(1, 2).copyOf(3))

        assertArrayNotSameButEquals(intArrayOf(1, 2), intArrayOf(1, 2, 3).copyOf(2))
        assertArrayNotSameButEquals(intArrayOf(1, 2, 0), intArrayOf(1, 2).copyOf(3))

        assertArrayNotSameButEquals(charArrayOf('A', 'B'), charArrayOf('A', 'B', 'C').copyOf(2))
        assertArrayNotSameButEquals(charArrayOf('A', 'B', '\u0000'), charArrayOf('A', 'B').copyOf(3))
    }

    test fun copyOfRange() {
        assertArrayNotSameButEquals(booleanArrayOf(true, false, true), booleanArrayOf(true, false, true, true).copyOfRange(0, 3))
        assertArrayNotSameButEquals(byteArrayOf(0, 1, 2), byteArrayOf(0, 1, 2, 3, 4, 5).copyOfRange(0, 3))
        assertArrayNotSameButEquals(shortArrayOf(0, 1, 2), shortArrayOf(0, 1, 2, 3, 4, 5).copyOfRange(0, 3))
        assertArrayNotSameButEquals(intArrayOf(0, 1, 2), intArrayOf(0, 1, 2, 3, 4, 5).copyOfRange(0, 3))
        assertArrayNotSameButEquals(longArrayOf(0, 1, 2), longArrayOf(0, 1, 2, 3, 4, 5).copyOfRange(0, 3))
        assertArrayNotSameButEquals(floatArrayOf(0F, 1F, 2F), floatArrayOf(0F, 1F, 2F, 3F, 4F, 5F).copyOfRange(0, 3))
        assertArrayNotSameButEquals(doubleArrayOf(0.0, 1.0, 2.0), doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0).copyOfRange(0, 3))
        assertArrayNotSameButEquals(charArrayOf('0', '1', '2'), charArrayOf('0', '1', '2', '3', '4', '5').copyOfRange(0, 3))
    }



    test fun reduce() {
        expect(-4) { intArrayOf(1, 2, 3) reduce { a, b -> a - b } }
        expect(-4.toLong()) { longArrayOf(1, 2, 3) reduce { a, b -> a - b } }
        expect(-4F) { floatArrayOf(1F, 2F, 3F) reduce { a, b -> a - b } }
        expect(-4.0) { doubleArrayOf(1.0, 2.0, 3.0) reduce { a, b -> a - b } }
        expect('3') { charArrayOf('1', '3', '2') reduce { a, b -> if (a > b) a else b } }
        expect(false) { booleanArrayOf(true, true, false) reduce { a, b -> a && b } }
        expect(true) { booleanArrayOf(true, true) reduce { a, b -> a && b } }
        expect(0.toByte()) { byteArrayOf(3, 2, 1) reduce { a, b -> (a - b).toByte() } }
        expect(0.toShort()) { shortArrayOf(3, 2, 1) reduce { a, b -> (a - b).toShort() } }

        assertTrue(fails {
            intArrayOf().reduce { a, b -> a + b }
        } is UnsupportedOperationException)
    }

    test fun reduceRight() {
        expect(2) { intArrayOf(1, 2, 3) reduceRight { a, b -> a - b } }
        expect(2.toLong()) { longArrayOf(1, 2, 3) reduceRight { a, b -> a - b } }
        expect(2F) { floatArrayOf(1F, 2F, 3F) reduceRight { a, b -> a - b } }
        expect(2.0) { doubleArrayOf(1.0, 2.0, 3.0) reduceRight { a, b -> a - b } }
        expect('3') { charArrayOf('1', '3', '2') reduceRight { a, b -> if (a > b) a else b } }
        expect(false) { booleanArrayOf(true, true, false) reduceRight { a, b -> a && b } }
        expect(true) { booleanArrayOf(true, true) reduceRight { a, b -> a && b } }
        expect(2.toByte()) { byteArrayOf(1, 2, 3) reduceRight { a, b -> (a - b).toByte() } }
        expect(2.toShort()) { shortArrayOf(1, 2, 3) reduceRight { a, b -> (a - b).toShort() } }

        assertTrue(fails {
            intArrayOf().reduceRight { a, b -> a + b }
        } is UnsupportedOperationException)
    }

    test fun reverse() {
        expect(listOf(3, 2, 1)) { intArrayOf(1, 2, 3).reverse() }
        expect(listOf<Byte>(3, 2, 1)) { byteArrayOf(1, 2, 3).reverse() }
        expect(listOf<Short>(3, 2, 1)) { shortArrayOf(1, 2, 3).reverse() }
        expect(listOf<Long>(3, 2, 1)) { longArrayOf(1, 2, 3).reverse() }
        expect(listOf(3F, 2F, 1F)) { floatArrayOf(1F, 2F, 3F).reverse() }
        expect(listOf(3.0, 2.0, 1.0)) { doubleArrayOf(1.0, 2.0, 3.0).reverse() }
        expect(listOf('3', '2', '1')) { charArrayOf('1', '2', '3').reverse() }
        expect(listOf(false, false, true)) { booleanArrayOf(true, false, false).reverse() }
    }

    test fun drop() {
        expect(listOf(1), { intArrayOf(1).drop(0) })
        expect(listOf(), { intArrayOf().drop(1) })
        expect(listOf(), { intArrayOf(1).drop(1) })
        expect(listOf(3), { intArrayOf(2, 3).drop(1) })
        expect(listOf(2000000000000), { longArrayOf(3000000000000, 2000000000000).drop(1) })
        expect(listOf(3.toByte()), { byteArrayOf(2, 3).drop(1) })
        expect(listOf(3.toShort()), { shortArrayOf(2, 3).drop(1) })
        expect(listOf(3.0f), { floatArrayOf(2f, 3f).drop(1) })
        expect(listOf(3.0), { doubleArrayOf(2.0, 3.0).drop(1) })
        expect(listOf(false), { booleanArrayOf(true, false).drop(1) })
        expect(listOf('b'), { charArrayOf('a', 'b').drop(1) })
        expect(listOf("b"), { arrayOf("a", "b").drop(1) })
        fails {
            listOf(2).drop(-1)
        }
    }

    test fun dropLast() {
        expect(listOf(), { intArrayOf().dropLast(1) })
        expect(listOf(), { intArrayOf(1).dropLast(1) })
        expect(listOf(1), { intArrayOf(1).dropLast(0) })
        expect(listOf(2), { intArrayOf(2, 3).dropLast(1) })
        expect(listOf(3000000000000), { longArrayOf(3000000000000, 2000000000000).dropLast(1) })
        expect(listOf(2.toByte()), { byteArrayOf(2, 3).dropLast(1) })
        expect(listOf(2.toShort()), { shortArrayOf(2, 3).dropLast(1) })
        expect(listOf(2.0f), { floatArrayOf(2f, 3f).dropLast(1) })
        expect(listOf(2.0), { doubleArrayOf(2.0, 3.0).dropLast(1) })
        expect(listOf(true), { booleanArrayOf(true, false).dropLast(1) })
        expect(listOf('a'), { charArrayOf('a', 'b').dropLast(1) })
        expect(listOf("a"), { arrayOf("a", "b").dropLast(1) })
        fails {
            listOf(1).dropLast(-1)
        }
    }

    test fun dropWhile() {
        expect(listOf(), { intArrayOf().dropWhile { it < 3 } })
        expect(listOf(), { intArrayOf(1).dropWhile { it < 3 } })
        expect(listOf(3, 1), { intArrayOf(2, 3, 1).dropWhile { it < 3 } })
        expect(listOf(2000000000000), { longArrayOf(3000000000000, 2000000000000).dropWhile { it > 2000000000000 } })
        expect(listOf(3.toByte(), 1.toByte()), { byteArrayOf(2, 3, 1).dropWhile { it < 3 } })
        expect(listOf(3.toShort(), 1.toShort()), { shortArrayOf(2, 3, 1).dropWhile { it < 3 } })
        expect(listOf(3f, 1f), { floatArrayOf(2f, 3f, 1f).dropWhile { it < 3 } })
        expect(listOf(3.0, 1.0), { doubleArrayOf(2.0, 3.0, 1.0).dropWhile { it < 3 } })
        expect(listOf(false, true), { booleanArrayOf(true, false, true).dropWhile { it } })
        expect(listOf('b', 'a'), { charArrayOf('a', 'b', 'a').dropWhile { it < 'b' } })
        expect(listOf("b", "a"), { arrayOf("a", "b", "a").dropWhile { it < "b" } })
    }

    test fun dropLastWhile() {
        expect(listOf(), { intArrayOf().dropLastWhile { it < 3 } })
        expect(listOf(), { intArrayOf(1).dropLastWhile { it < 3 } })
        expect(listOf(2, 3), { intArrayOf(2, 3, 1).dropLastWhile { it < 3 } })
        expect(listOf(3000000000000), { longArrayOf(3000000000000, 2000000000000).dropLastWhile { it < 3000000000000 } })
        expect(listOf(2.toByte(), 3.toByte()), { byteArrayOf(2, 3, 1).dropLastWhile { it < 3 } })
        expect(listOf(2.toShort(), 3.toShort()), { shortArrayOf(2, 3, 1).dropLastWhile { it < 3 } })
        expect(listOf(2f, 3f), { floatArrayOf(2f, 3f, 1f).dropLastWhile { it < 3 } })
        expect(listOf(2.0, 3.0), { doubleArrayOf(2.0, 3.0, 1.0).dropLastWhile { it < 3 } })
        expect(listOf(true, false), { booleanArrayOf(true, false, true).dropLastWhile { it } })
        expect(listOf('a', 'b'), { charArrayOf('a', 'b', 'a').dropLastWhile { it < 'b' } })
        expect(listOf("a", "b"), { arrayOf("a", "b", "a").dropLastWhile { it < "b" } })
    }

    test fun take() {
        expect(listOf(), { intArrayOf().take(1) })
        expect(listOf(), { intArrayOf(1).take(0) })
        expect(listOf(1), { intArrayOf(1).take(1) })
        expect(listOf(2), { intArrayOf(2, 3).take(1) })
        expect(listOf(3000000000000), { longArrayOf(3000000000000, 2000000000000).take(1) })
        expect(listOf(2.toByte()), { byteArrayOf(2, 3).take(1) })
        expect(listOf(2.toShort()), { shortArrayOf(2, 3).take(1) })
        expect(listOf(2.0f), { floatArrayOf(2f, 3f).take(1) })
        expect(listOf(2.0), { doubleArrayOf(2.0, 3.0).take(1) })
        expect(listOf(true), { booleanArrayOf(true, false).take(1) })
        expect(listOf('a'), { charArrayOf('a', 'b').take(1) })
        expect(listOf("a"), { arrayOf("a", "b").take(1) })
        fails {
            listOf(1).take(-1)
        }
    }

    test fun takeLast() {
        expect(listOf(), { intArrayOf().takeLast(1) })
        expect(listOf(), { intArrayOf(1).takeLast(0) })
        expect(listOf(1), { intArrayOf(1).takeLast(1) })
        expect(listOf(3), { intArrayOf(2, 3).takeLast(1) })
        expect(listOf(2000000000000), { longArrayOf(3000000000000, 2000000000000).takeLast(1) })
        expect(listOf(3.toByte()), { byteArrayOf(2, 3).takeLast(1) })
        expect(listOf(3.toShort()), { shortArrayOf(2, 3).takeLast(1) })
        expect(listOf(3.0f), { floatArrayOf(2f, 3f).takeLast(1) })
        expect(listOf(3.0), { doubleArrayOf(2.0, 3.0).takeLast(1) })
        expect(listOf(false), { booleanArrayOf(true, false).takeLast(1) })
        expect(listOf('b'), { charArrayOf('a', 'b').takeLast(1) })
        expect(listOf("b"), { arrayOf("a", "b").takeLast(1) })
        fails {
            listOf(1).takeLast(-1)
        }
    }

    test fun takeWhile() {
        expect(listOf(), { intArrayOf().takeWhile { it < 3 } })
        expect(listOf(1), { intArrayOf(1).takeWhile { it < 3 } })
        expect(listOf(2), { intArrayOf(2, 3, 1).takeWhile { it < 3 } })
        expect(listOf(3000000000000), { longArrayOf(3000000000000, 2000000000000).takeWhile { it > 2000000000000 } })
        expect(listOf(2.toByte()), { byteArrayOf(2, 3, 1).takeWhile { it < 3 } })
        expect(listOf(2.toShort()), { shortArrayOf(2, 3, 1).takeWhile { it < 3 } })
        expect(listOf(2f), { floatArrayOf(2f, 3f, 1f).takeWhile { it < 3 } })
        expect(listOf(2.0), { doubleArrayOf(2.0, 3.0, 1.0).takeWhile { it < 3 } })
        expect(listOf(true), { booleanArrayOf(true, false, true).takeWhile { it } })
        expect(listOf('a'), { charArrayOf('a', 'c', 'b').takeWhile { it < 'c' } })
        expect(listOf("a"), { arrayOf("a", "c", "b").takeWhile { it < "c" } })
    }

    test fun takeLastWhile() {
        expect(listOf(), { intArrayOf().takeLastWhile { it < 3 } })
        expect(listOf(1), { intArrayOf(1).takeLastWhile { it < 3 } })
        expect(listOf(1), { intArrayOf(2, 3, 1).takeLastWhile { it < 3 } })
        expect(listOf(2000000000000), { longArrayOf(3000000000000, 2000000000000).takeLastWhile { it < 3000000000000 } })
        expect(listOf(1.toByte()), { byteArrayOf(2, 3, 1).takeLastWhile { it < 3 } })
        expect(listOf(1.toShort()), { shortArrayOf(2, 3, 1).takeLastWhile { it < 3 } })
        expect(listOf(1f), { floatArrayOf(2f, 3f, 1f).takeLastWhile { it < 3 } })
        expect(listOf(1.0), { doubleArrayOf(2.0, 3.0, 1.0).takeLastWhile { it < 3 } })
        expect(listOf(true), { booleanArrayOf(true, false, true).takeLastWhile { it } })
        expect(listOf('b'), { charArrayOf('a', 'c', 'b').takeLastWhile { it < 'c' } })
        expect(listOf("b"), { arrayOf("a", "c", "b").takeLastWhile { it < "c" } })
    }

    test fun filter() {
        expect(listOf(), { intArrayOf().filter { it > 2 } })
        expect(listOf(), { intArrayOf(1).filter { it > 2 } })
        expect(listOf(3), { intArrayOf(2, 3).filter { it > 2 } })
        expect(listOf(3000000000000), { longArrayOf(3000000000000, 2000000000000).filter { it > 2000000000000 } })
        expect(listOf(3.toByte()), { byteArrayOf(2, 3).filter { it > 2 } })
        expect(listOf(3.toShort()), { shortArrayOf(2, 3).filter { it > 2 } })
        expect(listOf(3.0f), { floatArrayOf(2f, 3f).filter { it > 2 } })
        expect(listOf(3.0), { doubleArrayOf(2.0, 3.0).filter { it > 2 } })
        expect(listOf(true), { booleanArrayOf(true, false).filter { it } })
        expect(listOf('b'), { charArrayOf('a', 'b').filter { it > 'a' } })
        expect(listOf("b"), { arrayOf("a", "b").filter { it > "a" } })
    }

    test fun filterNot() {
        expect(listOf(), { intArrayOf().filterNot { it > 2 } })
        expect(listOf(1), { intArrayOf(1).filterNot { it > 2 } })
        expect(listOf(2), { intArrayOf(2, 3).filterNot { it > 2 } })
        expect(listOf(2000000000000), { longArrayOf(3000000000000, 2000000000000).filterNot { it > 2000000000000 } })
        expect(listOf(2.toByte()), { byteArrayOf(2, 3).filterNot { it > 2 } })
        expect(listOf(2.toShort()), { shortArrayOf(2, 3).filterNot { it > 2 } })
        expect(listOf(2.0f), { floatArrayOf(2f, 3f).filterNot { it > 2 } })
        expect(listOf(2.0), { doubleArrayOf(2.0, 3.0).filterNot { it > 2 } })
        expect(listOf(false), { booleanArrayOf(true, false).filterNot { it } })
        expect(listOf('a'), { charArrayOf('a', 'b').filterNot { it > 'a' } })
        expect(listOf("a"), { arrayOf("a", "b").filterNot { it > "a" } })
    }

    test fun filterNotNull() {
        expect(listOf("a"), { arrayOf("a", null).filterNotNull() })
    }

    test fun asListPrimitives() {
        // Array of primitives
        val arr = intArrayOf(1, 2, 3, 4, 2, 5)
        val list = arr.asList()
        assertEquals(list, arr.toList())

        assertTrue(2 in list)
        assertFalse(0 in list)
        assertTrue(list.containsAll(listOf(5, 4, 3)))
        assertFalse(list.containsAll(listOf(5, 6, 3)))

        expect(1) { list.indexOf(2) }
        expect(4) { list.lastIndexOf(2) }
        expect(-1) { list.indexOf(6) }

        assertEquals(list.subList(3, 5), listOf(4, 2))

        val iter = list.listIterator(2)
        expect(2) { iter.nextIndex() }
        expect(1) { iter.previousIndex() }
        expect(3) {
            iter.next()
            iter.previous()
            iter.next()
        }

        arr[2] = 4
        assertEquals(list, arr.toList())

        assertEquals(IntArray(0).asList(), emptyList<Int>())
    }

    test fun asListObjects() {
        val arr = arrayOf("a", "b", "c", "d", "b", "e")
        val list = arr.asList()

        assertEquals(list, arr.toList())

        assertTrue("b" in list)
        assertFalse("z" in list)

        expect(1) { list.indexOf("b") }
        expect(4) { list.lastIndexOf("b") }
        expect(-1) { list.indexOf("x") }

        assertTrue(list.containsAll(listOf("e", "d", "c")))
        assertFalse(list.containsAll(listOf("e", "x", "c")))

        assertEquals(list.subList(3, 5), listOf("d", "b"))

        val iter = list.listIterator(2)
        expect(2) { iter.nextIndex() }
        expect(1) { iter.previousIndex() }
        expect("c") {
            iter.next()
            iter.previous()
            iter.next()
        }

        arr[2] = "xx"
        assertEquals(list, arr.toList())

        assertEquals(Array(0, { "" }).asList(), emptyList<String>())
    }

    test fun sort() {
        val intArr = intArrayOf(5, 2, 1, 9, 80, Int.MIN_VALUE, Int.MAX_VALUE)
        intArr.sort()
        assertArrayNotSameButEquals(intArrayOf(Int.MIN_VALUE, 1, 2, 5, 9, 80, Int.MAX_VALUE), intArr)

        val longArr = longArrayOf(200, 2, 1, 4, 3, Long.MIN_VALUE, Long.MAX_VALUE)
        longArr.sort()
        assertArrayNotSameButEquals(longArrayOf(Long.MIN_VALUE, 1, 2, 3, 4, 200, Long.MAX_VALUE), longArr)

        val charArr = charArrayOf('d', 'c', 'E', 'a', '\u0000', '\uFFFF')
        charArr.sort()
        assertArrayNotSameButEquals(charArrayOf('\u0000', 'E', 'a', 'c', 'd', '\uFFFF'), charArr)

        val strArr = arrayOf("9", "80", "all", "Foo")
        strArr.sort()
        assertArrayNotSameButEquals(arrayOf("80", "9", "Foo", "all"), strArr)
    }

    test fun sorted() {
        assertTrue(arrayOf<Long>().sorted().none())
        assertEquals(listOf(1), arrayOf(1).sorted())

        arrayOf("ac", "aD", "aba").let {
            it.sorted().assertSorted { a, b -> a <= b }
            it.sortedDescending().assertSorted { a, b -> a >= b }
        }

        intArrayOf(3, 7, 1).let {
            it.sorted().assertSorted { a, b -> a <= b }
            it.sortedDescending().assertSorted { a, b -> a >= b }
        }

        longArrayOf(1, Long.MIN_VALUE, Long.MAX_VALUE).let {
            it.sorted().assertSorted { a, b -> a <= b }
            it.sortedDescending().assertSorted { a, b -> a >= b }
        }

        charArrayOf('a', 'D', 'c').let {
            it.sorted().assertSorted { a, b -> a <= b }
            it.sortedDescending().assertSorted { a, b -> a >= b }
        }

        byteArrayOf(1, Byte.MAX_VALUE, Byte.MIN_VALUE).let {
            it.sorted().assertSorted { a, b -> a <= b }
            it.sortedDescending().assertSorted { a, b -> a >= b }
        }

        doubleArrayOf(Double.POSITIVE_INFINITY, 1.0, Double.MAX_VALUE).let {
            it.sorted().assertSorted { a, b -> a <= b }
            it.sortedDescending().assertSorted { a, b -> a >= b }
        }
    }

    test fun sortedBy() {
        val values = arrayOf("ac", "aD", "aba")
        val indices = values.indices.toList().toIntArray()

        assertEquals(listOf(1, 2, 0), indices.sortedBy { values[it] })
    }

    test fun sortedNullableBy() {
        fun String.nullIfEmpty() = if (isEmpty()) null else this
        arrayOf(null, "").let {
            expect(listOf(null, "")) { it.sortedWith(nullsFirst(compareBy { it })) }
            expect(listOf("", null)) { it.sortedWith(nullsLast(compareByDescending { it })) }
            expect(listOf("", null)) { it.sortedWith(nullsLast(compareByDescending { it.nullIfEmpty() })) }
        }
    }

    test fun sortedWith() {
        assertEquals(listOf(3, 0, 4, 1, 5, 2), intArrayOf(0, 1, 2, 3, 4, 5).sortedWith( compareBy { it: Int -> it % 3 }.thenByDescending { it } ))
    }
}
