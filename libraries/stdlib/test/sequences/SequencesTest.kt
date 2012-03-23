package test.sequences

import kotlin.sequences.grouped
import kotlin.sequences.sequence
import kotlin.sequences.sliding
import kotlin.sequences.times

import kotlin.test.assertEquals
import org.junit.Test

class SequencesTest {

  Test fun timesComputesTheCartesianSquareOfAnIterable() {
    val expected = sequence(
        #(1, 1), #(1, 2), #(1, 3),
        #(2, 1), #(2, 2), #(2, 3),
        #(3, 1), #(3, 2), #(3, 3)
    )
    assertEquals(expected, (1..3) * (1..3))
  }

  Test fun timesComputesTheCartesianProductOfTwoRanges() {
    val expected = sequence(
        #(1, 4), #(1, 5), #(1, 6), #(1, 7),
        #(2, 4), #(2, 5), #(2, 6), #(2, 7),
        #(3, 4), #(3, 5), #(3, 6), #(3, 7)
    )
    assertEquals(expected, (1..3) * (4..7))
  }

  Test fun timesComputesTheCartesianProductOfTwoIterables() {
    val expected = sequence(
        #(1, 4), #(1, 5), #(1, 6),
        #(3, 4), #(3, 5), #(3, 6),
        #(5, 4), #(5, 5), #(5, 6),
        #(7, 4), #(7, 5), #(7, 6)
    )
    assertEquals(expected, sequence(1, 3, 5, 7) * (4..6))
  }

  private val digits = """
62229893423380308135336276614282806444486645238749
30358907296290491560440772390713810515859307960866
701724271218839987979087922749219016997
""".trim().replaceAll("\\n", "")

  Test fun groupedReturnsFixedSizeGroupsOfStrings() {
    val actual = digits.grouped(50).toList().get(2)
    assertEquals("701724271218839987979087922749219016997", actual)
  }

  Test fun slidingOverALongStringOfDigits() {
    val actual = digits.sliding(3).filter { Integer.parseInt(it) > 990 }
    assertEquals(sequence("998", "997"), actual)
  }
}
