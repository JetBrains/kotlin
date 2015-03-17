package test.collections

import org.junit.Test as test
import kotlin.test.assertEquals

class SequenceJVMTest {

    test fun filterIsInstance() {
        val src: Sequence<Any> = listOf(1, 2, 3.toDouble(), "abc", "cde").sequence()

        val intValues: Sequence<Int> = src.filterIsInstance<Int>()
        assertEquals(listOf(1, 2), intValues.toArrayList())

        val doubleValues: Sequence<Double> = src.filterIsInstance<Double>()
        assertEquals(listOf(3.0), doubleValues.toArrayList())

        val stringValues: Sequence<String> = src.filterIsInstance<String>()
        assertEquals(listOf("abc", "cde"), stringValues.toArrayList())

        val anyValues: Sequence<Any> = src.filterIsInstance<Any>()
        assertEquals(src.toList(), anyValues.toArrayList())

        val charValues: Sequence<Char> = src.filterIsInstance<Char>()
        assertEquals(0, charValues.toArrayList().size())
    }
}
