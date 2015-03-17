package iterators

import kotlin.test.assertEquals
import org.junit.Test as test
import kotlin.test.failsWith

class IteratorsJVMTest {


    test fun flatMapAndTakeExtractTheTransformedElements() {
        fun intToBinaryDigits() = { (i: Int) ->
            val binary = Integer.toBinaryString(i)!!
            var index = 0
            sequence<Char> { if (index < binary.length()) binary.get(index++) else null }
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

    test fun flatMapOnIterator() {
        val result = sequenceOf(1, 2).flatMap { i -> (0..i).sequence()}
        assertEquals(listOf(0, 1, 0, 1, 2), result.toList())
    }
}
