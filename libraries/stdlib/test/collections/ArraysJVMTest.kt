package test.collections

import kotlin.test.*
import org.junit.Test as test

class ArraysJVMTest {

    test fun sort() {
        var a = intArray(5, 2, 1, 4, 3)
        var b = intArray(1, 2, 3, 4, 5)
        a.sort()
        for (i in a.indices)
            expect(b[i]) { a[i] }
    }

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
        expect('3') { charArray('1', '3', '2') reduce { a, b -> if (a > b) a else b } }
        expect(false) { booleanArray(true, true, false) reduce { a, b -> a && b } }
        expect(true) { booleanArray(true, true) reduce { a, b -> a && b } }
        expect(0.toByte()) { byteArray(3, 2, 1) reduce { a, b -> (a - b).toByte() } }
        expect(0.toShort()) { shortArray(3, 2, 1) reduce { a, b -> (a - b).toShort() } }

        failsWith (javaClass<UnsupportedOperationException>()) {
            intArray().reduce { a, b -> a + b }
        }
    }

    test fun reduceRight() {
        expect(2) { intArray(1, 2, 3) reduceRight { a, b -> a - b } }
        expect(2.toLong()) { longArray(1, 2, 3) reduceRight { a, b -> a - b } }
        expect(2.toFloat()) { floatArray(1.toFloat(), 2.toFloat(), 3.toFloat()) reduceRight { a, b -> a - b } }
        expect(2.0) { doubleArray(1.0, 2.0, 3.0) reduceRight { a, b -> a - b } }
        expect('3') { charArray('1', '3', '2') reduceRight { a, b -> if (a > b) a else b } }
        expect(false) { booleanArray(true, true, false) reduceRight { a, b -> a && b } }
        expect(true) { booleanArray(true, true) reduceRight { a, b -> a && b } }
        expect(2.toByte()) { byteArray(1, 2, 3) reduceRight { a, b -> (a - b).toByte() } }
        expect(2.toShort()) { shortArray(1, 2, 3) reduceRight { a, b -> (a - b).toShort() } }

        failsWith (javaClass<UnsupportedOperationException>()) {
            intArray().reduceRight { a, b -> a + b }
        }
    }

    test fun indexOf() {
        expect(-1) { byteArray(1, 2, 3) indexOf 0 }
        expect(0) { byteArray(1, 2, 3) indexOf 1 }
        expect(1) { byteArray(1, 2, 3) indexOf 2 }
        expect(2) { byteArray(1, 2, 3) indexOf 3 }

        expect(-1) { shortArray(1, 2, 3) indexOf 0 }
        expect(0) { shortArray(1, 2, 3) indexOf 1 }
        expect(1) { shortArray(1, 2, 3) indexOf 2 }
        expect(2) { shortArray(1, 2, 3) indexOf 3 }

        expect(-1) { intArray(1, 2, 3) indexOf 0 }
        expect(0) { intArray(1, 2, 3) indexOf 1 }
        expect(1) { intArray(1, 2, 3) indexOf 2 }
        expect(2) { intArray(1, 2, 3) indexOf 3 }

        expect(-1) { longArray(1, 2, 3) indexOf 0 }
        expect(0) { longArray(1, 2, 3) indexOf 1 }
        expect(1) { longArray(1, 2, 3) indexOf 2 }
        expect(2) { longArray(1, 2, 3) indexOf 3 }

        expect(-1) { floatArray(1.0f, 2.0f, 3.0f) indexOf 0f }
        expect(0) { floatArray(1.0f, 2.0f, 3.0f) indexOf 1.0f }
        expect(1) { floatArray(1.0f, 2.0f, 3.0f) indexOf 2.0f }
        expect(2) { floatArray(1.0f, 2.0f, 3.0f) indexOf 3.0f }

        expect(-1) { doubleArray(1.0, 2.0, 3.0) indexOf 0.0 }
        expect(0) { doubleArray(1.0, 2.0, 3.0) indexOf 1.0 }
        expect(1) { doubleArray(1.0, 2.0, 3.0) indexOf 2.0 }
        expect(2) { doubleArray(1.0, 2.0, 3.0) indexOf 3.0 }

        expect(-1) { charArray('a', 'b', 'c') indexOf 'z' }
        expect(0) { charArray('a', 'b', 'c') indexOf 'a' }
        expect(1) { charArray('a', 'b', 'c') indexOf 'b' }
        expect(2) { charArray('a', 'b', 'c') indexOf 'c' }

        expect(0) { booleanArray(true, false) indexOf true }
        expect(1) { booleanArray(true, false) indexOf false }
        expect(-1) { booleanArray(true) indexOf false }
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
        assertFalse(floatArray(0.1.toFloat()).isEmpty())
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
        expect(2.0.toFloat(), { floatArray(3.0.toFloat(), 2.0.toFloat()).min() })
        expect(2.0, { doubleArray(2.0, 3.0).min() })
        expect('a', { charArray('a', 'b').min() })
    }

    test fun max() {
        expect(null, { intArray().max() })
        expect(1, { intArray(1).max() })
        expect(3, { intArray(2, 3).max() })
        expect(3000000000000, { longArray(3000000000000, 2000000000000).max() })
        expect(3, { byteArray(1, 3, 2).max() })
        expect(3, { shortArray(3, 2).max() })
        expect(3.0.toFloat(), { floatArray(3.0.toFloat(), 2.0.toFloat()).max() })
        expect(3.0, { doubleArray(2.0, 3.0).max() })
        expect('b', { charArray('a', 'b').max() })
    }

    test fun minBy() {
        expect(null, { intArray().minBy { it } })
        expect(1, { intArray(1).minBy { it } })
        expect(3, { intArray(2, 3).minBy { -it } })
        expect(2000000000000, { longArray(3000000000000, 2000000000000).minBy { it + 1 } })
        expect(1, { byteArray(1, 3, 2).minBy { it * it } })
        expect(3, { shortArray(3, 2).minBy { "a" } })
        expect(2.0.toFloat(), { floatArray(3.0.toFloat(), 2.0.toFloat()).minBy { it.toString() } })
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
        expect(3.0.toFloat(), { floatArray(3.0.toFloat(), 2.0.toFloat()).maxBy { it.toString() } })
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
        expect(3.0.toFloat()) { floatArray(1.0.toFloat(), 2.0.toFloat()).sum() }
    }

    test fun orEmptyNull() {
        val x: Array<String>? = null
        val y: Array<out String>? = null
        val xArray = x.orEmpty()
        val yArray = y.orEmpty()
        expect(0) { xArray.size() }
        expect(0) { yArray.size() }
    }

    test fun orEmptyNotNull() {
        val x: Array<String>? = array("1", "2")
        val xArray = x.orEmpty()
        expect(2) { xArray.size() }
        expect("1") { xArray[0] }
        expect("2") { xArray[1] }
    }

    test fun drop() {
        expect(listOf(), { intArray().drop(1) })
        expect(listOf(), { intArray(1).drop(1) })
        expect(listOf(3), { intArray(2, 3).drop(1) })
        expect(listOf(2000000000000), { longArray(3000000000000, 2000000000000).drop(1) })
        expect(listOf(3.toByte()), { byteArray(2, 3).drop(1) })
        expect(listOf(3.toShort()), { shortArray(2, 3).drop(1) })
        expect(listOf(3.0f), { floatArray(2f, 3f).drop(1) })
        expect(listOf(3.0), { doubleArray(2.0, 3.0).drop(1) })
        expect(listOf(false), { booleanArray(true, false).drop(1) })
        expect(listOf('b'), { charArray('a', 'b').drop(1) })
        expect(listOf("b"), { array("a", "b").drop(1) })
    }

    test fun dropWhile() {
        expect(listOf(), { intArray().dropWhile { it < 3 } })
        expect(listOf(), { intArray(1).dropWhile { it < 3 } })
        expect(listOf(3, 1), { intArray(2, 3, 1).dropWhile { it < 3 } })
        expect(listOf(2000000000000), { longArray(3000000000000, 2000000000000).dropWhile { it > 2000000000000 } })
        expect(listOf(3.toByte(), 1.toByte()), { byteArray(2, 3, 1).dropWhile { it < 3 } })
        expect(listOf(3.toShort(), 1.toShort()), { shortArray(2, 3, 1).dropWhile { it < 3 } })
        expect(listOf(3f, 1f), { floatArray(2f, 3f, 1f).dropWhile { it < 3 } })
        expect(listOf(3.0, 1.0), { doubleArray(2.0, 3.0, 1.0).dropWhile { it < 3 } })
        expect(listOf(false, true), { booleanArray(true, false, true).dropWhile { it } })
        expect(listOf('b', 'a'), { charArray('a', 'b', 'a').dropWhile { it < 'b' } })
        expect(listOf("b", "a"), { array("a", "b", "a").dropWhile { it < "b" } })
    }

    test fun take() {
        expect(listOf(), { intArray().take(1) })
        expect(listOf(1), { intArray(1).take(1) })
        expect(listOf(2), { intArray(2, 3).take(1) })
        expect(listOf(3000000000000), { longArray(3000000000000, 2000000000000).take(1) })
        expect(listOf(2.toByte()), { byteArray(2, 3).take(1) })
        expect(listOf(2.toShort()), { shortArray(2, 3).take(1) })
        expect(listOf(2.0f), { floatArray(2f, 3f).take(1) })
        expect(listOf(2.0), { doubleArray(2.0, 3.0).take(1) })
        expect(listOf(true), { booleanArray(true, false).take(1) })
        expect(listOf('a'), { charArray('a', 'b').take(1) })
        expect(listOf("a"), { array("a", "b").take(1) })
    }

    test fun takeWhile() {
        expect(listOf(), { intArray().takeWhile { it < 3 } })
        expect(listOf(1), { intArray(1).takeWhile { it < 3 } })
        expect(listOf(2), { intArray(2, 3, 1).takeWhile { it < 3 } })
        expect(listOf(3000000000000), { longArray(3000000000000, 2000000000000).takeWhile { it > 2000000000000 } })
        expect(listOf(2.toByte()), { byteArray(2, 3, 1).takeWhile { it < 3 } })
        expect(listOf(2.toShort()), { shortArray(2, 3, 1).takeWhile { it < 3 } })
        expect(listOf(2f), { floatArray(2f, 3f, 1f).takeWhile { it < 3 } })
        expect(listOf(2.0), { doubleArray(2.0, 3.0, 1.0).takeWhile { it < 3 } })
        expect(listOf(true), { booleanArray(true, false, true).takeWhile { it } })
        expect(listOf('a'), { charArray('a', 'b', 'a').takeWhile { it < 'b' } })
        expect(listOf("a"), { array("a", "b", "a").takeWhile { it < "b" } })
    }

    test fun filter() {
        expect(listOf(), { intArray().filter { it > 2 } })
        expect(listOf(), { intArray(1).filter { it > 2 } })
        expect(listOf(3), { intArray(2, 3).filter { it > 2 } })
        expect(listOf(3000000000000), { longArray(3000000000000, 2000000000000).filter { it > 2000000000000 } })
        expect(listOf(3.toByte()), { byteArray(2, 3).filter { it > 2 } })
        expect(listOf(3.toShort()), { shortArray(2, 3).filter { it > 2 } })
        expect(listOf(3.0f), { floatArray(2f, 3f).filter { it > 2 } })
        expect(listOf(3.0), { doubleArray(2.0, 3.0).filter { it > 2 } })
        expect(listOf(true), { booleanArray(true, false).filter { it } })
        expect(listOf('b'), { charArray('a', 'b').filter { it > 'a' } })
        expect(listOf("b"), { array("a", "b").filter { it > "a" } })
    }

    test fun filterNot() {
        expect(listOf(), { intArray().filterNot { it > 2 } })
        expect(listOf(1), { intArray(1).filterNot { it > 2 } })
        expect(listOf(2), { intArray(2, 3).filterNot { it > 2 } })
        expect(listOf(2000000000000), { longArray(3000000000000, 2000000000000).filterNot { it > 2000000000000 } })
        expect(listOf(2.toByte()), { byteArray(2, 3).filterNot { it > 2 } })
        expect(listOf(2.toShort()), { shortArray(2, 3).filterNot { it > 2 } })
        expect(listOf(2.0f), { floatArray(2f, 3f).filterNot { it > 2 } })
        expect(listOf(2.0), { doubleArray(2.0, 3.0).filterNot { it > 2 } })
        expect(listOf(false), { booleanArray(true, false).filterNot { it } })
        expect(listOf('a'), { charArray('a', 'b').filterNot { it > 'a' } })
        expect(listOf("a"), { array("a", "b").filterNot { it > "a" } })
    }

    test fun filterNotNull() {
        expect(listOf("a"), { array("a", null).filterNotNull() })
    }

    test fun asListPrimitives() {
        // Array of primitives
        val arr = intArray(1, 2, 3, 4, 2, 5)
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
        val arr = array("a", "b", "c", "d", "b", "e")
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
}
