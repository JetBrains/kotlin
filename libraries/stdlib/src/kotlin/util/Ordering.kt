/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin

import java.util.Comparator

/**
 * Compares two values using the sequence of functions to calculate a result of comparison.
 */
public fun <T : Any> compareValuesBy(a: T?, b: T?, vararg functions: (T) -> Comparable<*>?): Int {
    require(functions.size() > 0)
    if (a identityEquals b) return 0
    if (a == null) return -1
    if (b == null) return 1
    for (fn in functions) {
        val v1 = fn(a)
        val v2 = fn(b)
        val diff = compareValues(v1, v2)
        if (diff != 0) return diff
    }
    return 0
}

/**
 * Compares two [Comparable] nullable values, null is considered less than any value.
 */
public fun <T : Comparable<*>> compareValues(a: T?, b: T?): Int {
    if (a identityEquals b) return 0
    if (a == null) return -1
    if (b == null) return 1

    return (a as Comparable<Any?>).compareTo(b)
}

/**
 * Creates a comparator using the sequence of functions to calculate a result of comparison.
 */
public fun <T> compareBy(vararg functions: (T) -> Comparable<*>?): Comparator<T> {
    return object : Comparator<T> {
        public override fun compare(a: T, b: T): Int = compareValuesBy(a, b, *functions)
    }
}

/**
 * Creates a comparator using the sequence of functions to calculate a result of comparison.
 */
deprecated("Use compareBy() instead")
public fun <T> comparator(vararg functions: (T) -> Comparable<*>?): Comparator<T> = compareBy(*functions)

/**
 * Creates a comparator using the function to transform value to a [Comparable] instance for comparison
 */
inline public fun <T> compareBy(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) comparable: (T) -> Comparable<*>): Comparator<T> {
    return object : Comparator<T> {
        public override fun compare(a: T, b: T): Int = compareValues(comparable(a), comparable(b))
    }
}

/**
 * Creates a descending comparator using the function to transform value to a [Comparable] instance for comparison
 */
inline public fun <T> compareByDescending(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) comparable: (T) -> Comparable<*>): Comparator<T> {
    return object : Comparator<T> {
        public override fun compare(a: T, b: T): Int = compareValues(comparable(b), comparable(a))
    }
}

/**
 * Creates a comparator using the primary comparator and
 * the function to transform value to a [Comparable] instance for comparison
 */
inline public fun <T> Comparator<T>.thenBy(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) comparable: (T) -> Comparable<*>): Comparator<T> {
    return object : Comparator<T> {
        public override fun compare(a: T, b: T): Int {
            val previousCompare = this@thenBy.compare(a, b)
            return if (previousCompare != 0) previousCompare else compareValues(comparable(a), comparable(b))
        }
    }
}

/**
 * Creates a descending comparator using the primary comparator and
 * the function to transform value to a [Comparable] instance for comparison
 */
inline public fun <T> Comparator<T>.thenByDescending(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) comparable: (T) -> Comparable<*>): Comparator<T> {
    return object : Comparator<T> {
        public override fun compare(a: T, b: T): Int {
            val previousCompare = this@thenByDescending.compare(a, b)
            return if (previousCompare != 0) previousCompare else compareValues(comparable(b), comparable(a))
        }
    }
}

/**
 * Creates a comparator using the function to calculate a result of comparison.
 */
inline public fun <T> comparator(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) comparison: (T, T) -> Int): Comparator<T> {
    return object : Comparator<T> {
        public override fun compare(a: T, b: T): Int = comparison(a, b)
    }
}

/**
 * Creates a comparator using the primary comparator and function to calculate a result of comparison.
 */
inline public fun <T> Comparator<T>.thenComparator(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) comparison: (T, T) -> Int): Comparator<T> {
    return object : Comparator<T> {
        public override fun compare(a: T, b: T): Int {
            val previousCompare = this@thenComparator.compare(a, b)
            return if (previousCompare != 0) previousCompare else comparison(a, b)
        }
    }
}