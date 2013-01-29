package test.arrays

import kotlin.test.*
import org.junit.Test as test

class ArraysJVMTest {

    test fun copyOf() {
        checkContent(booleanArray(true, false, true, false, true, false).copyOf().iterator(), 6) { it % 2 == 0 }
        checkContent(byteArray(0, 1, 2, 3, 4, 5).copyOf().iterator(), 6) { it.toByte() }
        checkContent(shortArray(0, 1, 2, 3, 4, 5).copyOf().iterator(), 6) { it.toShort() }
        checkContent(intArray(0, 1, 2, 3, 4, 5).copyOf().iterator(), 6) { it }
        checkContent(longArray(0, 1, 2, 3, 4, 5).copyOf().iterator(), 6) { it.toLong() }
        checkContent(floatArray(0.toFloat(), 1.toFloat(), 2.toFloat(), 3.toFloat()).copyOf().iterator(), 4) { it.toFloat() }
        checkContent(doubleArray(0.0, 1.0, 2.0, 3.0, 4.0, 5.0).copyOf().iterator(), 6) { it.toDouble() }
        checkContent(charArray('0', '1', '2', '3', '4', '5').copyOf().iterator(), 6) { (it + '0').toChar() }
    }

    test fun copyOfRange() {
        checkContent(booleanArray(true, false, true, false, true, false).copyOfRange(0, 3).iterator(), 3) { it % 2 == 0 }
        checkContent(byteArray(0, 1, 2, 3, 4, 5).copyOfRange(0, 3).iterator(), 3) { it.toByte() }
        checkContent(shortArray(0, 1, 2, 3, 4, 5).copyOfRange(0, 3).iterator(), 3) { it.toShort() }
        checkContent(intArray(0, 1, 2, 3, 4, 5).copyOfRange(0, 3).iterator(), 3) { it }
        checkContent(longArray(0, 1, 2, 3, 4, 5).copyOfRange(0, 3).iterator(), 3) { it.toLong() }
        checkContent(floatArray(0.toFloat(), 1.toFloat(), 2.toFloat(), 3.toFloat()).copyOfRange(0, 3).iterator(), 3) { it.toFloat() }
        checkContent(doubleArray(0.0, 1.0, 2.0, 3.0, 4.0, 5.0).copyOfRange(0, 3).iterator(), 3) { it.toDouble() }
        checkContent(charArray('0', '1', '2', '3', '4', '5').copyOfRange(0, 3).iterator(), 3) { (it + '0').toChar() }
    }

    test fun reduce() {
        expect(-4) { intArray(1, 2, 3) reduce { a, b -> a - b } }
        expect(-4.toLong()) { longArray(1, 2, 3) reduce { a, b -> a - b } }
        expect(-4.toFloat()) { floatArray(1.toFloat(), 2.toFloat(), 3.toFloat()) reduce { a, b -> a - b } }
        expect(-4.0) { doubleArray(1.0, 2.0, 3.0) reduce { a, b -> a - b } }
        expect('3') { charArray('1', '3', '2') reduce { a, b -> if(a > b) a else b } }
        expect(false) { booleanArray(true, true, false) reduce { a, b -> a && b } }
        expect(true) { booleanArray(true, true) reduce { a, b -> a && b } }
        expect(0.toByte()) { byteArray(3, 2, 1) reduce { a, b -> (a - b).toByte() } }
        expect(0.toShort()) { shortArray(3, 2, 1) reduce { a, b -> (a - b).toShort() } }

        failsWith (javaClass<UnsupportedOperationException>()) {
            intArray().reduce { a, b -> a + b}
        }
    }

    test fun reduceRight() {
        expect(2) { intArray(1, 2, 3) reduceRight { a, b -> a - b } }
        expect(2.toLong()) { longArray(1, 2, 3) reduceRight { a, b -> a - b } }
        expect(2.toFloat()) { floatArray(1.toFloat(), 2.toFloat(), 3.toFloat()) reduceRight { a, b -> a - b } }
        expect(2.0) { doubleArray(1.0, 2.0, 3.0) reduceRight { a, b -> a - b } }
        expect('3') { charArray('1', '3', '2') reduceRight { a, b -> if(a > b) a else b } }
        expect(false) { booleanArray(true, true, false) reduceRight { a, b -> a && b } }
        expect(true) { booleanArray(true, true) reduceRight { a, b -> a && b } }
        expect(2.toByte()) { byteArray(1, 2, 3) reduceRight { a, b -> (a - b).toByte() } }
        expect(2.toShort()) { shortArray(1, 2, 3) reduceRight { a, b -> (a - b).toShort() } }

        failsWith (javaClass<UnsupportedOperationException>()) {
            intArray().reduceRight { a, b -> a + b}
        }
    }
}
