package kotlin

import kotlin.comparisons.Comparator

/**
 * Classes which inherit from this interface have a defined total ordering between their instances.
 */
public interface Comparable<in T> {
    /**
     * Compares this object with the specified object for order. Returns zero if this object is equal
     * to the specified [other] object, a negative number if it's less than [other], or a positive number
     * if it's greater than [other].
     */
    public operator fun compareTo(other: T): Int
}

typealias Comparator<T> = kotlin.comparisons.Comparator<T>

