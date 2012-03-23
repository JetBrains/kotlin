package test.sequences

import kotlin.sequences.asSequence
import kotlin.sequences.empty
import kotlin.sequences.sequence

import kotlin.test.assertEquals
import org.junit.Test

class SequenceTest {

  Test fun filterReturnsTheExpectedSequence() {
    val actual = (1..10).asSequence().filter { it % 2 == 0 }
    assertEquals(sequence(2, 4, 6, 8, 10), actual)
  }

  Test fun filterReturnsAnEmptySequenceWhenNoElementMatchesThePredicate() {
    assertEquals(empty<Int>(), sequence(1, 3, 4, 7).filter { it > 7 })
  }

  Test fun foldReturnsTheExpectedReduction() {
    val actual = sequence(1, 3, 5, 7, 9).fold(-10, sum)
    assertEquals(-10 + 1 + 3 + 5 + 7 + 9, actual)
  }

  Test fun foldReturnsTheInitialValueForAnEmptySequence() {
    val initialValue = -10
    assertEquals(initialValue, empty<Int>().fold(initialValue, sum))
  }

  private val sum = { (a : Int, b : Int) -> a + b }

  Test fun mapReturnsTheExpectedTransformation() {
    assertEquals(sequence(2, 6, 8, 14), sequence(1, 3, 4, 7).map { it * 2 })
  }

  Test fun takeReturnsTheFirstNElements() {
    assertEquals(sequence(2, 1, 3), sequence(2, 1, 3, 5, 4).take(3))
  }

  Test fun takeReturnsTheEntireSequenceWhenTheNumberOfElementsIsGreaterThanItsSize() {
    assertEquals(sequence(1, 2), sequence(1, 2).take(3))
  }

  Test fun takeReturnsAnEmptySequenceForAnEmptySequence() {
    assertEquals(empty<Int>(), empty<Int>().take(3))
  }

  Test fun takeReturnsAnEmptySequenceWhenTheNumberOfElementsToExtractIsZero() {
    assertEquals(empty<Int>(), sequence(1, 2).take(0))
  }

  Test fun takeReturnsAnEmptySequenceWhenTheNumberOfElementsToExtractIsNegative() {
    assertEquals(empty<Int>(), sequence(1, 2).take(-1))
  }

  Test fun takeWhileReturnsTheFirstElementsMatchingAGivenPredicate() {
    val actual = sequence(1, 2, 3, 4, 1, 2, 3, 4).takeWhile { it < 3 }
    assertEquals(sequence(1, 2), actual)
  }

  Test fun takeWhileReturnsTheEntireSequenceWhenThePredicateMatchesAllElements() {
    assertEquals(sequence(1, 2, 3), sequence(1, 2, 3).takeWhile { it < 9 })
  }

  Test fun takeWhileReturnsAnEmptySequenceWhenThePredicateMatchesNothing() {
    assertEquals(empty<Int>(), sequence(1, 2, 3).takeWhile { it < 0 })
  }
}
