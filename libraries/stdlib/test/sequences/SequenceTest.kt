package test.sequences

import java.util.ArrayList

import kotlin.sequences.asSequence
import kotlin.sequences.empty
import kotlin.sequences.sequence
import kotlin.util.arrayList
import kotlin.util.fold

import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.junit.Test

class SequenceTest {

  Test fun filterReturnsTheExpectedSequence() {
    val actual = (1..10).asSequence().filter { it % 2 == 0 }
    assertEquals(sequence(2, 4, 6, 8, 10), actual)
  }

  Test fun filterReturnsAnEmptySequenceWhenNoElementMatchesThePredicate() {
    val actual = sequence(1, 3, 4, 7).filter { it > 7 }
    assertEquals(empty<Int>(), actual)
  }

  Test fun foldReturnsTheExpectedReduction() {
    val actual = (1..10).asSequence().filter { it % 2 == 1 }.fold(0, sum)
    assertEquals(1 + 3 + 5 + 7 + 9, actual)
  }

  Test fun foldReturnsTheInitialValueForAnEmptySequence() {
    val initialValue = 0
    val actual = (1..0).asSequence().fold(initialValue, sum)

    assertEquals(initialValue, actual)
  }

  private val sum = { (a : Int, b : Int) -> a + b }

  Test fun mapReturnsTheExpectedTransformation() {
    val actual = sequence(1, 3, 4, 7).map { it * 2 }
    assertEquals(sequence(2, 6, 8, 14), actual)
  }

  Test fun takeReturnsTheFirstNElements() {
    var actual = (1..5).asSequence().take(3)
    assertEquals(sequence(1, 2, 3), actual)
  }

  Test fun takeReturnsTheEntireSequenceWhenTheNumberOfElementsIsGreaterThanItsSize() {
    val actual = sequence(1, 2).take(3)
    assertEquals(sequence(1, 2), actual)
  }

  Test fun takeReturnsAnEmptySequenceForAnEmptySequence() {
    assertEquals(empty<Int>(), empty<Int>().take(3))
  }

  Test fun takeReturnsAnEmptySequenceWhenTheNumberOfElementsToExtractIsZero() {
    val actual = sequence(1, 2).take(0)
    assertEquals(empty<Int>(), actual)
  }

  Test fun takeReturnsAnEmptySequenceWhenTheNumberOfElementsToExtractIsNegative() {
    val actual = sequence(1, 2).take(-1)
    assertEquals(empty<Int>(), actual)
  }

  Test fun takeWhileReturnsTheFirstElementsMatchingAGivenPredicate() {
    val actual = sequence(1, 2, 3, 4, 1, 2, 3, 4).takeWhile { it < 3 }
    assertEquals(sequence(1, 2), actual)
  }

  Test fun takeWhileReturnsTheEntireSequenceWhenThePredicateMatchesAllElements() {
    val actual = sequence(1, 2, 3).takeWhile { it < 9 }
    assertEquals(sequence(1, 2, 3), actual)
  }

  Test fun takeWhileReturnsAnEmptySequenceWhenThePredicateMatchesNothing() {
    val actual = sequence(1, 2, 3).takeWhile { it < 0 }
    assertEquals(empty<Int>(), actual)
  }
}
