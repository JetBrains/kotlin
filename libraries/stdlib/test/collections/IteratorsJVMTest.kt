package test.collections

import org.junit.Test as test
import kotlin.test.*
import java.util.*

class IteratorsJVMTest {

    test fun testEnumeration() {
        val v = Vector<Int>()
        for(i in 1..5)
            v.add(i)

        var sum = 0
        for(k in v.elements())
            sum += k

        assertEquals(15, sum)
    }

    test fun flatMapAndTakeExtractTheTransformedElements() {
        fun intToBinaryDigits() = { (i: Int) ->
            val binary = Integer.toBinaryString(i)!!
            var index = 0
            stream<Char> { if (index < binary.length()) binary.get(index++) else null }
        }

        val expected = arrayListOf(
                      '0', // fibonacci(0) = 0
                      '1', // fibonacci(1) = 1
                      '1', // fibonacci(2) = 1
                 '1', '0', // fibonacci(3) = 2
                 '1', '1', // fibonacci(4) = 3
            '1', '0', '1'  // fibonacci(5) = 5
        )

        assertEquals(expected, fibonacci().flatMap<Int, Char>(intToBinaryDigits()).take(10).toList())
    }

    test fun flatMapOnStream() {
        val result = listOf(1, 2).stream().flatMap<Int, Int> { (0..it).stream() }
        assertEquals(listOf(0, 1, 0, 1, 2), result.toList())
    }
}
