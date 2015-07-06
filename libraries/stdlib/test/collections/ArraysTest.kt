package test.collections

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.expect
import org.junit.Test as test

fun <T> checkContent(iter: Iterator<T>, length: Int, value: (Int) -> T) {
    var idx = 0
    while (idx != length && iter.hasNext()) {
        assertEquals(value(idx++), iter.next(), "Invalid element")
    }

    assertEquals(length, idx, "Invalid length")
    assertFalse(iter.hasNext(), "Invalid length (hasNext())")
}

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

        val expected: Float = 0.0.toFloat()
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

    test fun max() {
        expect(null, { arrayOf<Int>().max() })
        expect(1, { arrayOf(1).max() })
        expect(3, { arrayOf(2, 3).max() })
        expect(3000000000000, { arrayOf(3000000000000, 2000000000000).max() })
        expect('b', { arrayOf('a', 'b').max() })
        expect("b", { arrayOf("a", "b").max() })
    }

    test fun minBy() {
        expect(null, { arrayOf<Int>().minBy { it } })
        expect(1, { arrayOf(1).minBy { it } })
        expect(3, { arrayOf(2, 3).minBy { -it } })
        expect('a', { arrayOf('a', 'b').minBy { "x$it" } })
        expect("b", { arrayOf("b", "abc").minBy { it.length() } })
    }

    test fun maxBy() {
        expect(null, { arrayOf<Int>().maxBy { it } })
        expect(1, { arrayOf(1).maxBy { it } })
        expect(2, { arrayOf(2, 3).maxBy { -it } })
        expect('b', { arrayOf('a', 'b').maxBy { "x$it" } })
        expect("abc", { arrayOf("b", "abc").maxBy { it.length() } })
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
        expect(3.0.toFloat()) { arrayOf<Float>(1.0.toFloat(), 2.0.toFloat()).sum() }
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

    test fun plus() {
        assertEquals(listOf("1", "2", "3", "4"), listOf("1", "2") + arrayOf("3", "4"))
        val arr = arrayOf("1", "2") + arrayOf("3", "4")
        assertEquals(4, arr.size())
        checkContent(arr.iterator(), 4) { (it + 1).toString() }
    }

    test fun plusVararg() {
        fun stringOnePlus(vararg a: String) = arrayOf("1") + a
        fun numberOnePlus(vararg a: Int) = intArrayOf(1) + a

        val arrS = stringOnePlus("2")
        checkContent(arrS.iterator(), 2) { (it + 1).toString() }

        val arrN = numberOnePlus(2)
        checkContent(arrN.iterator(), 2) { it + 1 }
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

    test fun toSortedList() {
        assertTrue(arrayOf<Long>().toSortedList().none())
        assertEquals(listOf(1), arrayOf(1).toSortedList())
        assertEquals(listOf("aab", "aba", "ac"), arrayOf("ac", "aab", "aba").toSortedList())
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

    /*

    TODO FIXME ASAP: These currently fail on JS due to missing upto() method on numbers

    test fun reduce() {
        expect(-4) { intArray(1, 2, 3) reduce { a, b -> a - b } }
        // Fails in JS: expect(-4.toLong()) { longArray(1, 2, 3) reduce { a, b -> a - b } }
        expect(-4.toFloat()) { floatArray(1.toFloat(), 2.toFloat(), 3.toFloat()) reduce { a, b -> a - b } }
        expect(-4.0) { doubleArray(1.0, 2.0, 3.0) reduce { a, b -> a - b } }
        expect('3') { charArray('1', '3', '2') reduce { a, b -> if(a > b) a else b } }
        expect(false) { booleanArray(true, true, false) reduce { a, b -> a && b } }
        expect(true) { booleanArray(true, true) reduce { a, b -> a && b } }
        //  Fails in JS: expect(0.toByte()) { byteArray(3, 2, 1) reduce { a, b -> (a - b).toByte() } }
        //  Fails in JS: expect(0.toShort()) { shortArray(3, 2, 1) reduce { a, b -> (a - b).toShort() } }

        failsWith(javaClass<UnsupportedOperationException>()) {
            intArray().reduce { a, b -> a + b}
        }
    }

    test fun reduceRight() {
        expect(2) { intArray(1, 2, 3) reduceRight { a, b -> a - b } }
        //  Fails in JS: expect(2.toLong()) { longArray(1, 2, 3) reduceRight { a, b -> a - b } }
        expect(2.toFloat()) { floatArray(1.toFloat(), 2.toFloat(), 3.toFloat()) reduceRight { a, b -> a - b } }
        expect(2.0) { doubleArray(1.0, 2.0, 3.0) reduceRight { a, b -> a - b } }
        expect('3') { charArray('1', '3', '2') reduceRight { a, b -> if(a > b) a else b } }
        expect(false) { booleanArray(true, true, false) reduceRight { a, b -> a && b } }
        expect(true) { booleanArray(true, true) reduceRight { a, b -> a && b } }
        //  Fails in JS: expect(2.toByte()) { byteArray(1, 2, 3) reduceRight { a, b -> (a - b).toByte() } }
        //  Fails in JS: expect(2.toShort()) { shortArray(1, 2, 3) reduceRight { a, b -> (a - b).toShort() } }

        failsWith(javaClass<UnsupportedOperationException>()) {
            intArray().reduceRight { a, b -> a + b}
        }
    }

    test fun reverse() {
        expect(arrayList(3, 2, 1)) { intArray(1, 2, 3).reverse() }
        //  Fails in JS: expect(arrayList<Byte>(3, 2, 1)) { byteArray(1, 2, 3).reverse() }
        //  Fails in JS: expect(arrayList<Short>(3, 2, 1)) { shortArray(1, 2, 3).reverse() }
        //  Fails in JS: expect(arrayList<Long>(3, 2, 1)) { longArray(1, 2, 3).reverse() }
        expect(arrayList(3.toFloat(), 2.toFloat(), 1.toFloat())) { floatArray(1.toFloat(), 2.toFloat(), 3.toFloat()).reverse() }
        expect(arrayList(3.0, 2.0, 1.0)) { doubleArray(1.0, 2.0, 3.0).reverse() }
        expect(arrayList('3', '2', '1')) { charArray('1', '2', '3').reverse() }
        expect(arrayList(false, false, true)) { booleanArray(true, false, false).reverse() }
    }

    */
}
