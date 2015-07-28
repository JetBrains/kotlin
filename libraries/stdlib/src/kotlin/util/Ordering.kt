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
import kotlin.platform.platformName

/**
 * Compares two values using the specified sequence of functions to calculate the result of the comparison.
 * The functions are called sequentially, receive the given values [a] and [b] and return [Comparable]
 * objects. As soon as the [Comparable] instances returned by a function for [a] and [b] values do not
 * compare as equal, the result of that comparison is returned.
 */
deprecated("Use selector functions accepting nullable T as a receiver.")
public fun <T : Any> compareValuesBy(a: T?, b: T?, vararg functions: (T) -> Comparable<*>?): Int {
    require(functions.size() > 0)
    if (a === b) return 0
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
 * Compares two values using the specified sequence of functions to calculate the result of the comparison.
 * The functions are called sequentially, receive the given values [a] and [b] and return [Comparable]
 * objects. As soon as the [Comparable] instances returned by a function for [a] and [b] values do not
 * compare as equal, the result of that comparison is returned.
 */
// TODO: Remove platformName after M13
platformName("compareValuesByNullable")
public fun <T> compareValuesBy(a: T, b: T, vararg selectors: (T) -> Comparable<*>?): Int {
    require(selectors.size() > 0)
    for (fn in selectors) {
        val v1 = fn(a)
        val v2 = fn(b)
        val diff = compareValues(v1, v2)
        if (diff != 0) return diff
    }
    return 0
}

/**
 * Compares two values using the specified [selector] function to calculate the result of the comparison.
 * The function is applied to the given values [a] and [b] and return [Comparable] objects.
 * The result of comparison of these [Comparable] instances is returned.
 */
public inline fun <T> compareValuesBy(a: T, b: T, selector: (T) -> Comparable<*>?): Int {
    return compareValues(selector(a), selector(b))
}

// Not so useful without type inference for receiver of expression
// compareValuesWith(v1, v2, compareBy { it.prop1 } thenByDescending { it.prop2 })
///**
// * Compares two values using the specified [comparator].
// */
//@suppress("NOTHING_TO_INLINE")
//public inline fun <T> compareValuesWith(a: T, b: T, comparator: Comparator<T>): Int = comparator.compare(a, b)

/**
 * Compares two nullable [Comparable] values. Null is considered less than any value.
 */
public fun <T : Comparable<*>> compareValues(a: T?, b: T?): Int {
    if (a === b) return 0
    if (a == null) return -1
    if (b == null) return 1

    return (a as Comparable<Any?>).compareTo(b)
}

/**
 * Creates a comparator using the sequence of functions to calculate a result of comparison.
 * The functions are called sequentially, receive the given values `a` and `b` and return [Comparable]
 * objects. As soon as the [Comparable] instances returned by a function for `a` and `b` values do not
 * compare as equal, the result of that comparison is returned from the [Comparator].
 */
public fun <T> compareBy(vararg functions: (T) -> Comparable<*>?): Comparator<T> {
    return object : Comparator<T> {
        public override fun compare(a: T, b: T): Int = compareValuesBy(a, b, *functions)
    }
}

/**
 * Creates a comparator using the sequence of functions to calculate a result of comparison.
 */
deprecated("Use compareBy() instead", ReplaceWith("compareBy(*functions)"))
public fun <T> comparator(vararg functions: (T) -> Comparable<*>?): Comparator<T> = compareBy(*functions)

/**
 * Creates a comparator using the function to transform value to a [Comparable] instance for comparison.
 */
inline public fun <T> compareBy(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) comparable: (T) -> Comparable<*>?): Comparator<T> {
    return object : Comparator<T> {
        public override fun compare(a: T, b: T): Int = compareValuesBy(a, b, comparable)
    }
}

/**
 * Creates a descending comparator using the function to transform value to a [Comparable] instance for comparison.
 */
inline public fun <T> compareByDescending(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) comparable: (T) -> Comparable<*>?): Comparator<T> {
    return object : Comparator<T> {
        public override fun compare(a: T, b: T): Int = compareValuesBy(b, a, comparable)
    }
}

/**
 * Creates a comparator using the primary comparator and
 * the function to transform value to a [Comparable] instance for comparison.
 */
inline public fun <T> Comparator<T>.thenBy(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) comparable: (T) -> Comparable<*>?): Comparator<T> {
    return object : Comparator<T> {
        public override fun compare(a: T, b: T): Int {
            val previousCompare = this@thenBy.compare(a, b)
            return if (previousCompare != 0) previousCompare else compareValuesBy(a, b, comparable)
        }
    }
}

/**
 * Creates a descending comparator using the primary comparator and
 * the function to transform value to a [Comparable] instance for comparison.
 */
inline public fun <T> Comparator<T>.thenByDescending(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) comparable: (T) -> Comparable<*>?): Comparator<T> {
    return object : Comparator<T> {
        public override fun compare(a: T, b: T): Int {
            val previousCompare = this@thenByDescending.compare(a, b)
            return if (previousCompare != 0) previousCompare else compareValuesBy(b, a, comparable)
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

// Not so useful without type inference for receiver of expression
/**
 * Extends the given comparator of non-nullable values to a comparator of nullable values
 * considering null value less than any other value.
 */
public fun <T: Any> Comparator<T>.nullsFirst(): Comparator<T?> {
    return object: Comparator<T?> {
        override fun compare(a: T?, b: T?): Int {
            if (a === b) return 0
            if (a == null) return -1
            if (b == null) return 1
            return this@nullsFirst.compare(a, b)
        }
    }
}

/**
 * Extends the given comparator of non-nullable values to a comparator of nullable values
 * considering null value greater than any other value.
 */
public fun <T: Any> Comparator<T>.nullsLast(): Comparator<T?> {
    return object: Comparator<T?> {
        override fun compare(a: T?, b: T?): Int {
            if (a === b) return 0
            if (a == null) return 1
            if (b == null) return -1
            return this@nullsLast.compare(a, b)
        }
    }
}