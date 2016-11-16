@file:kotlin.jvm.JvmVersion
package test.collections

import org.junit.Test
import kotlin.test.assertEquals

class SequenceJVMTest {

    @Test fun filterIsInstance() {
        val src: Sequence<Any> = listOf(1, 2, 3.toDouble(), "abc", "cde").asSequence()

        val intValues: Sequence<Int> = src.filterIsInstance<Int>()
        assertEquals(listOf(1, 2), intValues.toList())

        val doubleValues: Sequence<Double> = src.filterIsInstance<Double>()
        assertEquals(listOf(3.0), doubleValues.toList())

        val stringValues: Sequence<String> = src.filterIsInstance<String>()
        assertEquals(listOf("abc", "cde"), stringValues.toList())

        val anyValues: Sequence<Any> = src.filterIsInstance<Any>()
        assertEquals(src.toList(), anyValues.toList())

        val charValues: Sequence<Char> = src.filterIsInstance<Char>()
        assertEquals(0, charValues.toList().size)
    }
}
