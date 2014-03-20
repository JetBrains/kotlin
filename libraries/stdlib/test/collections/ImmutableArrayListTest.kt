package test.collections

import kotlin.test.*

import junit.framework.TestCase
import java.util.Random

class ImmutableArrayListTest() : TestCase() {

    fun testSimple() {
        val builder = ImmutableArrayListBuilder<Int>()
        builder.add(17)
        val list = builder.build()
        assertEquals(1, list.size())
        assertEquals(17, list[0])
    }


    fun testGet() {
        for (length in 0 .. 55) {
            val list = buildIntArray(length, 19)
            assertEquals(length, list.size)
            checkList(list, length, 19)
        }
    }

    private fun buildIntArray(length: Int, firstValue: Int): List<Int> {
        val builder = ImmutableArrayListBuilder<Int>()
        for (j in 0 .. length - 1) {
            builder.add(firstValue + j)
        }
        return builder.build()
    }


    private fun checkList(list: List<Int>, expectedLength: Int, expectedFirstValue: Int) {
        assertEquals(expectedLength, list.size)
        for (i in 0 .. expectedLength - 1) {
            assertEquals(expectedFirstValue + i, list[i])
        }
        try {
            list[expectedLength]
            fail()
        } catch (e: IndexOutOfBoundsException) {
            // expected
        }
    }


    fun testSublist() {
        val r = Random(1)
        for (i in 0 .. 200) {
            val length = r.nextInt(55)
            val list = buildIntArray(length, 23)
            val fromIndex = r.nextInt(length + 1)
            val toIndex = fromIndex + r.nextInt(length - fromIndex + 1)
            val sublist = list.subList(fromIndex, toIndex)
            checkList(sublist, toIndex - fromIndex, 23 + fromIndex)
        }
    }

}
