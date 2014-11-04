package test.collections

import org.junit.Test as test
import kotlin.test.assertEquals

class StreamJVMTest {

    test fun filterIsInstance() {
        val src: Stream<Any> = listOf(1,2,3.toDouble(), "abc", "cde").stream()

        val ints: Stream<Int> = src.filterIsInstance<Int>()
        assertEquals(arrayListOf(1,2), ints.toArrayList())

        val doubles: Stream<Double> = src.filterIsInstance<Double>()
        assertEquals(arrayListOf(3.0), doubles.toArrayList())

        val strings: Stream<String> = src.filterIsInstance<String>()
        assertEquals(arrayListOf("abc", "cde"), strings.toArrayList())

        val anys: Stream<Any> = src.filterIsInstance<Any>()
        assertEquals(src.toList(), anys.toArrayList())

        val chars: Stream<Char> = src.filterIsInstance<Char>()
        assertEquals(0, chars.toArrayList().size())
    }
}
