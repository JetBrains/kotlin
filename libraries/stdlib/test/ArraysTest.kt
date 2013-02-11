package test.arrays

import kotlin.test.*
import org.junit.Test as test

fun <T> checkContent(iter : Iterator<T>, length : Int, value : (Int) -> T) {
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

        val arr2 = Array<String>(0, {"$it"})
        assertEquals(-1, arr2.lastIndex)
    }

    test fun arrayLastIndex() {
        val arr1 = intArray(0, 1, 2, 3, 4)
        assertEquals(4, arr1.lastIndex)
        assertEquals(4, arr1[arr1.lastIndex])

        val arr2 = Array<String>(5, {"$it"})
        assertEquals(4, arr2.lastIndex)
        assertEquals("4", arr2[arr2.lastIndex])
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
