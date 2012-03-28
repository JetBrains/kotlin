package iterators

import kotlin.test.assertEquals
import org.junit.Test

class IteratorsTest {

    private fun fibonacci(): java.util.Iterator<Int> {
        // fibonacci terms
        var index = 0; var a = 0; var b = 1
        return iterate<Int> { when (index++) { 0 -> a; 1 -> b; else -> { val result = a + b; a = b; b = result; result } } }
    }

    Test fun filterAndTakeWhileExtractTheElementsWithinRange() {
        assertEquals(arrayList(144, 233, 377, 610, 987), fibonacci().filter { it > 100 }.takeWhile { it < 1000 }.toList())
    }

    Test fun foldReducesTheFirstNElements() {
        val sum = { (a: Int, b: Int) -> a + b }
        assertEquals(arrayList(13, 21, 34, 55, 89).fold(0, sum), fibonacci().filter { it > 10 }.take(5).fold(0, sum))
    }

    Test fun takeExtractsTheFirstNElements() {
        assertEquals(arrayList(0, 1, 1, 2, 3, 5, 8, 13, 21, 34), fibonacci().take(10).toList())
    }

    Test fun mapAndTakeWhileExtractTheTransformedElements() {
        assertEquals(arrayList(0, 3, 3, 6, 9, 15), fibonacci().map { it * 3 }.takeWhile { (i: Int) -> i < 20 }.toList())
    }

    Test fun flatMapAndTakeExtractTheTransformedElements() {
        fun intToBinaryDigits() = { (i: Int) ->
            val binary = Integer.toBinaryString(i).sure()
            var index = 0
            iterate<Char> { if (index < binary.length()) binary.get(index++) else null }
        }

        val expected = arrayList(
                      '0', // fibonacci(0) = 0
                      '1', // fibonacci(1) = 1
                      '1', // fibonacci(2) = 1
                 '1', '0', // fibonacci(3) = 2
                 '1', '1', // fibonacci(4) = 3
            '1', '0', '1'  // fibonacci(5) = 5
        )

        assertEquals(expected, fibonacci().flatMap<Int, Char>(intToBinaryDigits()).take(10).toList())
    }

    Test fun joinConcatenatesTheFirstNElementsAboveAThreshold() {
        assertEquals("13, 21, 34, 55, 89, ...", fibonacci().filter { it > 10 }.join(separator = ", ", limit = 5))
    }

    Test fun toStringJoinsNoMoreThanTheFirstTenElements() {
        assertEquals("0, 1, 1, 2, 3, 5, 8, 13, 21, 34, ...", fibonacci().toString())
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().filter {  it > 10 }.toString())
        assertEquals("144, 233, 377, 610, 987", fibonacci().filter { it > 100 }.takeWhile { it < 1000 }.toString())
    }
}
