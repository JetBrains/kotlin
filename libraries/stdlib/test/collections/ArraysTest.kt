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

        val arr2 = Array<String>(0, { "$it" })
        assertEquals(-1, arr2.lastIndex)
    }

    test fun arrayLastIndex() {
        val arr1 = intArray(0, 1, 2, 3, 4)
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
        expect(null, { array<Int>().min() })
        expect(1, { array(1).min() })
        expect(2, { array(2, 3).min() })
        expect(2000000000000, { array(3000000000000, 2000000000000).min() })
        expect('a', { array('a', 'b').min() })
        expect("a", { array("a", "b").min() })
    }

    test fun max() {
        expect(null, { array<Int>().max() })
        expect(1, { array(1).max() })
        expect(3, { array(2, 3).max() })
        expect(3000000000000, { array(3000000000000, 2000000000000).max() })
        expect('b', { array('a', 'b').max() })
        expect("b", { array("a", "b").max() })
    }

    test fun minBy() {
        expect(null, { array<Int>().minBy { it } })
        expect(1, { array(1).minBy { it } })
        expect(3, { array(2, 3).minBy { -it } })
        expect('a', { array('a', 'b').minBy { "x$it" } })
        expect("b", { array("b", "abc").minBy { it.length() } })
    }

    test fun maxBy() {
        expect(null, { array<Int>().maxBy { it } })
        expect(1, { array(1).maxBy { it } })
        expect(2, { array(2, 3).maxBy { -it } })
        expect('b', { array('a', 'b').maxBy { "x$it" } })
        expect("abc", { array("b", "abc").maxBy { it.length() } })
    }

    test fun minByEvaluateOnce() {
        var c = 0
        expect(1, { array(5, 4, 3, 2, 1).minBy { c++; it * it } })
        assertEquals(5, c)
    }

    test fun maxByEvaluateOnce() {
        var c = 0
        expect(5, { array(5, 4, 3, 2, 1).maxBy { c++; it * it } })
        assertEquals(5, c)
    }

    test fun sum() {
        expect(0) { array<Int>().sum() }
        expect(14) { array(2, 3, 9).sum() }
        expect(3.0) { array(1.0, 2.0).sum() }
        expect(200) { array<Byte>(100, 100).sum() }
        expect(50000) { array<Short>(20000, 30000).sum() }
        expect(3000000000000) { array<Long>(1000000000000, 2000000000000).sum() }
        expect(3.0.toFloat()) { array<Float>(1.0.toFloat(), 2.0.toFloat()).sum() }
    }

    test fun indexOf() {
        expect(-1) { array("cat", "dog", "bird").indexOf("mouse") }
        expect(0) { array("cat", "dog", "bird").indexOf("cat") }
        expect(1) { array("cat", "dog", "bird").indexOf("dog") }
        expect(2) { array("cat", "dog", "bird").indexOf("bird") }
        expect(0) { array(null, "dog", null).indexOf(null) }
    }

    test fun plus() {
        assertEquals(listOf("1", "2", "3", "4"), array("1", "2") + array("3", "4"))
        assertEquals(listOf("1", "2", "3", "4"), listOf("1", "2") + array("3", "4"))
    }

    test fun plusVararg() {
        fun onePlus(vararg a: String) = array("1") + a
        assertEquals(listOf("1", "2"), onePlus("2"))
    }

    test fun first() {
        expect(1) { array(1, 2, 3).first() }
        expect(2) { array(1, 2, 3).first { it % 2 == 0 } }
    }

    test fun last() {
        expect(3) { array(1, 2, 3).last() }
        expect(2) { array(1, 2, 3).last { it % 2 == 0 } }
    }

    test fun contains() {
        assertTrue(array("1", "2", "3", "4").contains("2"))
        assertTrue("3" in array("1", "2", "3", "4"))
        assertTrue("0" !in array("1", "2", "3", "4"))
    }

    test fun slice() {
        val iter = listOf(3, 1, 2)

        assertEquals(listOf("B"), array("A", "B", "C").slice(1..1))
        assertEquals(listOf('E', 'B', 'C'), array('A', 'B', 'C', 'E').slice(iter))

        assertEquals(listOf<Int>(), array<Int>().slice(5..4))
        assertEquals(listOf<Int>(), array(1, 2, 3).slice(5..1))
        assertEquals(listOf(2, 3, 9), array(2, 3, 9, 2, 3, 9).slice(iter))
        assertEquals(listOf(2.0, 3.0), array(2.0, 3.0, 9.0).slice(0..1))
        assertEquals(listOf(2f, 3f), array(2f, 3f, 9f).slice(0..1))
        assertEquals(listOf<Byte>(127, 100), array<Byte>(50, 100, 127).slice(2 downTo 1))
        assertEquals(listOf<Short>(200, 100), array<Short>(50, 100, 200).slice(2 downTo 1))
        assertEquals(listOf(100L, 200L, 30L), array(50L, 100L, 200L, 30L).slice(1..3))
        assertEquals(listOf(true, false, true), array(true, false, true, true).slice(iter))
    }

    test fun toSortedList() {
        assertTrue(array<Long>().toSortedList().none())
        assertEquals(listOf(1), array(1).toSortedList())
        assertEquals(listOf("aab", "aba", "ac"), array("ac", "aab", "aba").toSortedList())
    }

    test fun asIterable() {
        val arr1 = intArray(1, 2, 3, 4, 5)
        val iter1 = arr1.asIterable()
        assertEquals(arr1.toList(), iter1.toList())
        arr1[0] = 0
        assertEquals(arr1.toList(), iter1.toList())

        val arr2 = array("one", "two", "three")
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
