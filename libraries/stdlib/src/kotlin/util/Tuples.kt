package kotlin

import java.io.Serializable

/**
 * Represents a generic pair of two values.
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Pair exhibits value semantics, i.e. two pairs are equal if both components are equal.
 *
 * An example of decomposing it into values:
 * ${code test.tuples.PairTest.pairMultiAssignment}
 *
 * $constructor: Creates new instance of [Pair]
 * $first: First value
 * $second: Second value
 */
public data class Pair<out A, out B>(
        public val first: A,
        public val second: B
                                    ) : Serializable {

    /**
     * Returns string representation of the [Pair] including its [first] and [second] values.
     */
    public override fun toString(): String = "($first, $second)"
}

/**
 * Converts pair into a list
 */
public fun <T> Pair<T, T>.toList(): List<T> = listOf(first, second)

/**
 * Represents a triad of values
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Triple exhibits value semantics, i.e. two triples are equal if all three components are equal.
 * An example of decomposing it into values:
 * {code test.tuples.PairTest.pairMultiAssignment}
 *
 * $first: First value
 * $second: Second value
 * $third: Third value
 */
public data class Triple<out A, out B, out C>(
        public val first: A,
        public val second: B,
        public val third: C
                                             ) : Serializable {

    /**
     * Returns string representation of the [Triple] including its [first] and [second] values.
     */
    public override fun toString(): String = "($first, $second, $third)"
}

/**
 * Converts triple into a list
 */
public fun <T> Triple<T, T, T>.toList(): List<T> = listOf(first, second, third)
