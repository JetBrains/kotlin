package kotlin

import java.io.Serializable

/**
 * Represents a generic pair of two values.
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Pair exhibits value semantics, i.e. two pairs are equal if both components are equal.
 *
 * An example of decomposing it into values:
 * @sample test.tuples.PairTest.pairMultiAssignment
 *
 * @param A type of the first value
 * @param B type of the second value
 * @property first First value
 * @property second Second value
 * @constructor Creates a new instance of Pair.
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
 * Converts this pair into a list.
 */
public fun <T> Pair<T, T>.toList(): List<T> = listOf(first, second)

/**
 * Represents a triad of values
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Triple exhibits value semantics, i.e. two triples are equal if all three components are equal.
 * An example of decomposing it into values:
 * @sample test.tuples.TripleTest.tripleMultiAssignment
 *
 * @param A type of the first value
 * @param B type of the second value
 * @param C type of the third value
 * @property first First value
 * @property second Second value
 * @property third Third value
 */
public data class Triple<out A, out B, out C>(
        public val first: A,
        public val second: B,
        public val third: C
                                             ) : Serializable {

    /**
     * Returns string representation of the [Triple] including its [first], [second] and [third] values.
     */
    public override fun toString(): String = "($first, $second, $third)"
}

/**
 * Converts this triple into a list.
 */
public fun <T> Triple<T, T, T>.toList(): List<T> = listOf(first, second, third)
