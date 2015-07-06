package test.collections

import kotlin.test.*
import org.junit.Test as test

class ArraysJVMTest {

    test fun sort() {
        var a = intArrayOf(5, 2, 1, 4, 3)
        var b = intArrayOf(1, 2, 3, 4, 5)
        a.sort()
        for (i in a.indices)
            expect(b[i]) { a[i] }
    }



    test fun reduce() {
        expect(-4) { intArrayOf(1, 2, 3) reduce { a, b -> a - b } }
        expect(-4.toLong()) { longArrayOf(1, 2, 3) reduce { a, b -> a - b } }
        expect(-4.toFloat()) { floatArrayOf(1.toFloat(), 2.toFloat(), 3.toFloat()) reduce { a, b -> a - b } }
        expect(-4.0) { doubleArrayOf(1.0, 2.0, 3.0) reduce { a, b -> a - b } }
        expect('3') { charArrayOf('1', '3', '2') reduce { a, b -> if (a > b) a else b } }
        expect(false) { booleanArrayOf(true, true, false) reduce { a, b -> a && b } }
        expect(true) { booleanArrayOf(true, true) reduce { a, b -> a && b } }
        expect(0.toByte()) { byteArrayOf(3, 2, 1) reduce { a, b -> (a - b).toByte() } }
        expect(0.toShort()) { shortArrayOf(3, 2, 1) reduce { a, b -> (a - b).toShort() } }

        failsWith (javaClass<UnsupportedOperationException>()) {
            intArrayOf().reduce { a, b -> a + b }
        }
    }

    test fun reduceRight() {
        expect(2) { intArrayOf(1, 2, 3) reduceRight { a, b -> a - b } }
        expect(2.toLong()) { longArrayOf(1, 2, 3) reduceRight { a, b -> a - b } }
        expect(2.toFloat()) { floatArrayOf(1.toFloat(), 2.toFloat(), 3.toFloat()) reduceRight { a, b -> a - b } }
        expect(2.0) { doubleArrayOf(1.0, 2.0, 3.0) reduceRight { a, b -> a - b } }
        expect('3') { charArrayOf('1', '3', '2') reduceRight { a, b -> if (a > b) a else b } }
        expect(false) { booleanArrayOf(true, true, false) reduceRight { a, b -> a && b } }
        expect(true) { booleanArrayOf(true, true) reduceRight { a, b -> a && b } }
        expect(2.toByte()) { byteArrayOf(1, 2, 3) reduceRight { a, b -> (a - b).toByte() } }
        expect(2.toShort()) { shortArrayOf(1, 2, 3) reduceRight { a, b -> (a - b).toShort() } }

        failsWith (javaClass<UnsupportedOperationException>()) {
            intArrayOf().reduceRight { a, b -> a + b }
        }
    }

    test fun indexOf() {
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

    test fun isEmpty() {
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
        assertFalse(floatArrayOf(0.1.toFloat()).isEmpty())
        assertTrue(doubleArrayOf().isEmpty())
        assertFalse(doubleArrayOf(0.1).isEmpty())
        assertTrue(booleanArrayOf().isEmpty())
        assertFalse(booleanArrayOf(false).isEmpty())
    }

    test fun isNotEmpty() {
        assertFalse(intArrayOf().isNotEmpty())
        assertTrue(intArrayOf(1).isNotEmpty())
    }

    test fun min() {
        expect(null, { intArrayOf().min() })
        expect(1, { intArrayOf(1).min() })
        expect(2, { intArrayOf(2, 3).min() })
        expect(2000000000000, { longArrayOf(3000000000000, 2000000000000).min() })
        expect(1, { byteArrayOf(1, 3, 2).min() })
        expect(2, { shortArrayOf(3, 2).min() })
        expect(2.0.toFloat(), { floatArrayOf(3.0.toFloat(), 2.0.toFloat()).min() })
        expect(2.0, { doubleArrayOf(2.0, 3.0).min() })
        expect('a', { charArrayOf('a', 'b').min() })
    }

    test fun max() {
        expect(null, { intArrayOf().max() })
        expect(1, { intArrayOf(1).max() })
        expect(3, { intArrayOf(2, 3).max() })
        expect(3000000000000, { longArrayOf(3000000000000, 2000000000000).max() })
        expect(3, { byteArrayOf(1, 3, 2).max() })
        expect(3, { shortArrayOf(3, 2).max() })
        expect(3.0.toFloat(), { floatArrayOf(3.0.toFloat(), 2.0.toFloat()).max() })
        expect(3.0, { doubleArrayOf(2.0, 3.0).max() })
        expect('b', { charArrayOf('a', 'b').max() })
    }

    test fun minBy() {
        expect(null, { intArrayOf().minBy { it } })
        expect(1, { intArrayOf(1).minBy { it } })
        expect(3, { intArrayOf(2, 3).minBy { -it } })
        expect(2000000000000, { longArrayOf(3000000000000, 2000000000000).minBy { it + 1 } })
        expect(1, { byteArrayOf(1, 3, 2).minBy { it * it } })
        expect(3, { shortArrayOf(3, 2).minBy { "a" } })
        expect(2.0.toFloat(), { floatArrayOf(3.0.toFloat(), 2.0.toFloat()).minBy { it.toString() } })
        expect(2.0, { doubleArrayOf(2.0, 3.0).minBy { Math.sqrt(it) } })
    }

    test fun minIndex() {
        val a = intArrayOf(1, 7, 9, -42, 54, 93)
        expect(3, { a.indices.minBy { a[it] } })
    }

    test fun maxBy() {
        expect(null, { intArrayOf().maxBy { it } })
        expect(1, { intArrayOf(1).maxBy { it } })
        expect(2, { intArrayOf(2, 3).maxBy { -it } })
        expect(3000000000000, { longArrayOf(3000000000000, 2000000000000).maxBy { it + 1 } })
        expect(3, { byteArrayOf(1, 3, 2).maxBy { it * it } })
        expect(3, { shortArrayOf(3, 2).maxBy { "a" } })
        expect(3.0.toFloat(), { floatArrayOf(3.0.toFloat(), 2.0.toFloat()).maxBy { it.toString() } })
        expect(3.0, { doubleArrayOf(2.0, 3.0).maxBy { Math.sqrt(it) } })
    }

    test fun maxIndex() {
        val a = intArrayOf(1, 7, 9, 239, 54, 93)
        expect(3, { a.indices.maxBy { a[it] } })
    }

    test fun sum() {
        expect(0) { intArrayOf().sum() }
        expect(14) { intArrayOf(2, 3, 9).sum() }
        expect(3.0) { doubleArrayOf(1.0, 2.0).sum() }
        expect(200) { byteArrayOf(100, 100).sum() }
        expect(50000) { shortArrayOf(20000, 30000).sum() }
        expect(3000000000000) { longArrayOf(1000000000000, 2000000000000).sum() }
        expect(3.0.toFloat()) { floatArrayOf(1.0.toFloat(), 2.0.toFloat()).sum() }
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
        val x: Array<String>? = arrayOf("1", "2")
        val xArray = x.orEmpty()
        expect(2) { xArray.size() }
        expect("1") { xArray[0] }
        expect("2") { xArray[1] }
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
}
