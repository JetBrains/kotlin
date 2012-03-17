package test.arrays

import kotlin.test.*

import junit.framework.TestCase

class ArraysTest() : TestCase() {

    fun testCopyOf() {
        checkContent(booleanArray(true, false, true, false, true, false).copyOf().iterator(), 6) { it % 2 == 0 }
        checkContent(byteArray(0, 1, 2, 3, 4, 5).copyOf().iterator(), 6) { it.toByte() }
        checkContent(shortArray(0, 1, 2, 3, 4, 5).copyOf().iterator(), 6) { it.toShort() }
        checkContent(intArray(0, 1, 2, 3, 4, 5).copyOf().iterator(), 6) { it }
        checkContent(longArray(0, 1, 2, 3, 4, 5).copyOf().iterator(), 6) { it.toLong() }
        checkContent(floatArray(0.toFloat(), 1.toFloat(), 2.toFloat(), 3.toFloat()).copyOf().iterator(), 4) { it.toFloat() }
        checkContent(doubleArray(0.0, 1.0, 2.0, 3.0, 4.0, 5.0).copyOf().iterator(), 6) { it.toDouble() }
        checkContent(charArray('0', '1', '2', '3', '4', '5').copyOf().iterator(), 6) { (it + '0').toChar() }
    }

    fun testCopyOfRange() {
        checkContent(booleanArray(true, false, true, false, true, false).copyOfRange(0, 3).iterator(), 3) { it % 2 == 0 }
        checkContent(byteArray(0, 1, 2, 3, 4, 5).copyOfRange(0, 3).iterator(), 3) { it.toByte() }
        checkContent(shortArray(0, 1, 2, 3, 4, 5).copyOfRange(0, 3).iterator(), 3) { it.toShort() }
        checkContent(intArray(0, 1, 2, 3, 4, 5).copyOfRange(0, 3).iterator(), 3) { it }
        checkContent(longArray(0, 1, 2, 3, 4, 5).copyOfRange(0, 3).iterator(), 3) { it.toLong() }
        checkContent(floatArray(0.toFloat(), 1.toFloat(), 2.toFloat(), 3.toFloat()).copyOfRange(0, 3).iterator(), 3) { it.toFloat() }
        checkContent(doubleArray(0.0, 1.0, 2.0, 3.0, 4.0, 5.0).copyOfRange(0, 3).iterator(), 3) { it.toDouble() }
        checkContent(charArray('0', '1', '2', '3', '4', '5').copyOfRange(0, 3).iterator(), 3) { (it + '0').toChar() }
    }


    fun <T> checkContent(val iter : Iterator<T>, val length : Int, val value : (Int) -> T) {
        var idx = 0
        while (idx != length && iter.hasNext) {
            assertEquals(value(idx++), iter.next(), "Invalid element")
        }

        assertEquals(length, idx, "Invalid length")
        assertFalse(iter.hasNext, "Invalid length (hasNext)")
    }

}
