package test.collections

import org.junit.Test as test
import kotlin.test.assertEquals

class StreamJVMTest {

    test fun filterIsInstance() {
        val src: Stream<Any> = listOf(1, 2, 3.toDouble(), "abc", "cde").stream()

        val intValues: Stream<Int> = src.filterIsInstance<Int>()
        assertEquals(listOf(1, 2), intValues.toArrayList())

        val doubleValues: Stream<Double> = src.filterIsInstance<Double>()
        assertEquals(listOf(3.0), doubleValues.toArrayList())

        val stringValues: Stream<String> = src.filterIsInstance<String>()
        assertEquals(listOf("abc", "cde"), stringValues.toArrayList())

        val anyValues: Stream<Any> = src.filterIsInstance<Any>()
        assertEquals(src.toList(), anyValues.toArrayList())

        val charValues: Stream<Char> = src.filterIsInstance<Char>()
        assertEquals(0, charValues.toArrayList().size())
    }
}
