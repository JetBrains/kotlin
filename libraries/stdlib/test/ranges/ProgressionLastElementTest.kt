package test.ranges

import org.junit.Test
import kotlin.comparisons.*
import kotlin.test.assertEquals


class ProgressionLastElementTest  {

    private val MAX = Int.MAX_VALUE
    private val MIN = Int.MIN_VALUE

    private val INTERESTING = intArrayOf(MIN, MIN / 2, -239, -23, -1, 0, 1, 42, 239, MAX / 2, MAX)

    private fun doTest(start: Int, end: Int, increment: Int, expected: Int) {

        val actualInt = IntProgression.fromClosedRange(start, end, increment).last
        assertEquals(expected, actualInt)

        val actualLong = LongProgression.fromClosedRange(start.toLong(), end.toLong(), increment.toLong()).last
        assertEquals(expected.toLong(), actualLong)
    }

    @Test fun calculateFinalElement() {
        // start == end
        for (x in INTERESTING) {
            for (increment in INTERESTING)
                if (increment != 0) {
                    doTest(x, x, increment, x)
                }
        }

        // increment == 1
        for (start in INTERESTING.indices) {
            for (end in start..INTERESTING.size - 1) {
                doTest(INTERESTING[start], INTERESTING[end], 1, INTERESTING[end])
            }
        }

        // increment == -1
        for (end in INTERESTING.indices) {
            for (start in end..INTERESTING.size - 1) {
                doTest(INTERESTING[start], INTERESTING[end], -1, INTERESTING[end])
            }
        }

        // end == MAX
        doTest(0, MAX, MAX, MAX)
        doTest(0, MAX, MAX / 2, MAX - 1)
        doTest(MIN + 1, MAX, MAX, MAX)
        doTest(MAX - 7, MAX, 3, MAX - 1)
        doTest(MAX - 7, MAX, MAX, MAX - 7)

        // end == MIN
        doTest(0, MIN, MIN, MIN)
        doTest(0, MIN, MIN / 2, MIN)
        doTest(MAX, MIN, MIN, -1)
        doTest(MIN + 7, MIN, -3, MIN + 1)
        doTest(MIN + 7, MIN, MIN, MIN + 7)
    }

    @Test fun iterateToFinalElement() {
        // Small tests
        for (start in -5..4) {
            for (end in -5..4) {
                for (increment in -10..9) {
                    // Cut down incorrect test data
                    if (increment == 0) continue
                    if (increment > 0 != start <= end) continue

                    // Iterate over the progression and obtain the expected result
                    // println("$start,$end,$increment")
                    var x = start
                    while (true) {
                        val next = x + increment
                        if (next !in minOf(start, end)..maxOf(start, end)) break
                        x = next
                    }

                    doTest(start, end, increment, x)
                }
            }
        }
    }
}
