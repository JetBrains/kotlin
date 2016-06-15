package kotlin.jdk8.collections.test

import org.junit.Test
import kotlin.test.*
import java.util.function.UnaryOperator


class ListTest {

    @Test fun replaceAll() {
        val list = mutableListOf("ab", "cde", "x")
        list.replaceAll { it.length.toString() }

        val expected = listOf("2", "3", "1")
        assertEquals(expected, list)

        list.replaceAll(UnaryOperator.identity())
        assertEquals(expected, list)
    }
}