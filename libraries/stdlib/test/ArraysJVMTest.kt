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

    test fun isEmpty() {
        assertTrue(intArray().isEmpty())
        assertFalse(intArray(1).isEmpty())
        assertTrue(byteArray().isEmpty())
        assertFalse(byteArray(1).isEmpty())
        assertTrue(shortArray().isEmpty())
        assertFalse(shortArray(1).isEmpty())
        assertTrue(longArray().isEmpty())
        assertFalse(longArray(1).isEmpty())
        assertTrue(charArray().isEmpty())
        assertFalse(charArray('a').isEmpty())
        assertTrue(floatArray().isEmpty())
        assertFalse(floatArray(0.1).isEmpty())
        assertTrue(doubleArray().isEmpty())
        assertFalse(doubleArray(0.1).isEmpty())
        assertTrue(booleanArray().isEmpty())
        assertFalse(booleanArray(false).isEmpty())
    }

    test fun isNotEmpty() {
        assertFalse(intArray().isNotEmpty())
        assertTrue(intArray(1).isNotEmpty())
    }

    test fun min() {
        expect(null, { intArray().min() })
        expect(1, { intArray(1).min() })
        expect(2, { intArray(2, 3).min() })
        expect(2000000000000, { longArray(3000000000000, 2000000000000).min() })
        expect(1, { byteArray(1, 3, 2).min() })
        expect(2, { shortArray(3, 2).min() })
        expect(2.0, { floatArray(3.0, 2.0).min() })
        expect(2.0, { doubleArray(2.0, 3.0).min() })
    }

    test fun max() {
        expect(null, { intArray().max() })
        expect(1, { intArray(1).max() })
        expect(3, { intArray(2, 3).max() })
        expect(3000000000000, { longArray(3000000000000, 2000000000000).max() })
        expect(3, { byteArray(1, 3, 2).max() })
        expect(3, { shortArray(3, 2).max() })
        expect(3.0, { floatArray(3.0, 2.0).max() })
        expect(3.0, { doubleArray(2.0, 3.0).max() })
    }

    test fun minBy() {
        expect(null, { intArray().minBy { it } })
        expect(1, { intArray(1).minBy { it } })
        expect(3, { intArray(2, 3).minBy { -it } })
        expect(2000000000000, { longArray(3000000000000, 2000000000000).minBy { it + 1 } })
        expect(1, { byteArray(1, 3, 2).minBy { it * it } })
        expect(3, { shortArray(3, 2).minBy { "a" } })
        expect(2.0, { floatArray(3.0, 2.0).minBy { it.toString() } })
        expect(2.0, { doubleArray(2.0, 3.0).minBy { Math.sqrt(it) } })
    }

    test fun minIndex() {
        val a = intArray(1, 7, 9, -42, 54, 93)
        expect(3, { a.indices.minBy { a[it] } })
    }

    test fun maxBy() {
        expect(null, { intArray().maxBy { it } })
        expect(1, { intArray(1).maxBy { it } })
        expect(2, { intArray(2, 3).maxBy { -it } })
        expect(3000000000000, { longArray(3000000000000, 2000000000000).maxBy { it + 1 } })
        expect(3, { byteArray(1, 3, 2).maxBy { it * it } })
        expect(3, { shortArray(3, 2).maxBy { "a" } })
        expect(3.0, { floatArray(3.0, 2.0).maxBy { it.toString() } })
        expect(3.0, { doubleArray(2.0, 3.0).maxBy { Math.sqrt(it) } })
    }

    test fun maxIndex() {
        val a = intArray(1, 7, 9, 239, 54, 93)
        expect(3, { a.indices.maxBy { a[it] } })
    }

    test fun sum() {
        expect(0) { intArray().sum() }
        expect(14) { intArray(2, 3, 9).sum() }
        expect(3.0) { doubleArray(1.0, 2.0).sum() }
        expect(200) { byteArray(100, 100).sum() }
        expect(50000) { shortArray(20000, 30000).sum() }
        expect(3000000000000) { longArray(1000000000000, 2000000000000).sum() }
        expect(3.0) { floatArray(1.0, 2.0).sum() }
    }
}
