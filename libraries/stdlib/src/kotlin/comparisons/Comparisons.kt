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
@file:kotlin.jvm.JvmName("ComparisonsKt")
@file:kotlin.jvm.JvmMultifileClass

package kotlin.comparisons

/**
 * Compares two values using the specified functions [selectors] to calculate the result of the comparison.
 * The functions are called sequentially, receive the given values [a] and [b] and return [Comparable]
 * objects. As soon as the [Comparable] instances returned by a function for [a] and [b] values do not
 * compare as equal, the result of that comparison is returned.
 */
public fun <T> compareValuesBy(a: T, b: T, vararg selectors: (T) -> Comparable<*>?): Int {
    require(selectors.size > 0)
    return compareValuesByImpl(a, b, selectors)
}

private fun <T> compareValuesByImpl(a: T, b: T, selectors: Array<out (T)->Comparable<*>?>): Int {
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
@kotlin.internal.InlineOnly
public inline fun <T> compareValuesBy(a: T, b: T, selector: (T) -> Comparable<*>?): Int {
    return compareValues(selector(a), selector(b))
}

/**
 * Compares two values using the specified [selector] function to calculate the result of the comparison.
 * The function is applied to the given values [a] and [b] and return objects of type K which are then being
 * compared with the given [comparator].
 */
@kotlin.internal.InlineOnly
public inline fun <T, K> compareValuesBy(a: T, b: T, comparator: Comparator<in K>, selector: (T) -> K): Int {
    return comparator.compare(selector(a), selector(b))
}

//// Not so useful without type inference for receiver of expression
//// compareValuesWith(v1, v2, compareBy { it.prop1 } thenByDescending { it.prop2 })
///**
// * Compares two values using the specified [comparator].
// */
//@Suppress("NOTHING_TO_INLINE")
//public inline fun <T> compareValuesWith(a: T, b: T, comparator: Comparator<T>): Int = comparator.compare(a, b)
//


/**
 * Compares two nullable [Comparable] values. Null is considered less than any value.
 */
public fun <T : Comparable<*>> compareValues(a: T?, b: T?): Int {
    if (a === b) return 0
    if (a == null) return -1
    if (b == null) return 1

    @Suppress("UNCHECKED_CAST")
    return (a as Comparable<Any>).compareTo(b)
}

/**
 * Creates a comparator using the sequence of functions to calculate a result of comparison.
 * The functions are called sequentially, receive the given values `a` and `b` and return [Comparable]
 * objects. As soon as the [Comparable] instances returned by a function for `a` and `b` values do not
 * compare as equal, the result of that comparison is returned from the [Comparator].
 */
public fun <T> compareBy(vararg selectors: (T) -> Comparable<*>?): Comparator<T> {
    require(selectors.size > 0)
    return Comparator { a, b -> compareValuesByImpl(a, b, selectors) }
}



/**
 * Creates a comparator using the function to transform value to a [Comparable] instance for comparison.
 */
@kotlin.internal.InlineOnly
public inline fun <T> compareBy(crossinline selector: (T) -> Comparable<*>?): Comparator<T> =
        Comparator { a, b -> compareValuesBy(a, b, selector) }

/**
 * Creates a comparator using the [selector] function to transform values being compared and then applying
 * the specified [comparator] to compare transformed values.
 */
@kotlin.internal.InlineOnly
public inline fun <T, K> compareBy(comparator: Comparator<in K>, crossinline selector: (T) -> K): Comparator<T> =
        Comparator { a, b -> compareValuesBy(a, b, comparator, selector) }

/**
 * Creates a descending comparator using the function to transform value to a [Comparable] instance for comparison.
 */
@kotlin.internal.InlineOnly
public inline fun <T> compareByDescending(crossinline selector: (T) -> Comparable<*>?): Comparator<T> =
        Comparator { a, b -> compareValuesBy(b, a, selector) }

/**
 * Creates a descending comparator using the [selector] function to transform values being compared and then applying
 * the specified [comparator] to compare transformed values.
 *
 * Note that an order of [comparator] is reversed by this wrapper.
 */
@kotlin.internal.InlineOnly
public inline fun <T, K> compareByDescending(comparator: Comparator<in K>, crossinline selector: (T) -> K): Comparator<T> =
        Comparator { a, b -> compareValuesBy(b, a, comparator, selector) }

/**
 * Creates a comparator comparing values after the primary comparator defined them equal. It uses
 * the function to transform value to a [Comparable] instance for comparison.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Comparator<T>.thenBy(crossinline selector: (T) -> Comparable<*>?): Comparator<T> =
        Comparator { a, b ->
            val previousCompare = this@thenBy.compare(a, b)
            if (previousCompare != 0) previousCompare else compareValuesBy(a, b, selector)
        }

/**
 * Creates a comparator comparing values after the primary comparator defined them equal. It uses
 * the [selector] function to transform values and then compares them with the given [comparator].
 */
@kotlin.internal.InlineOnly
public inline fun <T, K> Comparator<T>.thenBy(comparator: Comparator<in K>, crossinline selector: (T) -> K): Comparator<T> =
        Comparator { a, b ->
            val previousCompare = this@thenBy.compare(a, b)
            if (previousCompare != 0) previousCompare else compareValuesBy(a, b, comparator, selector)
        }

/**
 * Creates a descending comparator using the primary comparator and
 * the function to transform value to a [Comparable] instance for comparison.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Comparator<T>.thenByDescending(crossinline selector: (T) -> Comparable<*>?): Comparator<T> =
        Comparator { a, b ->
            val previousCompare = this@thenByDescending.compare(a, b)
            if (previousCompare != 0) previousCompare else compareValuesBy(b, a, selector)
        }

/**
 * Creates a descending comparator comparing values after the primary comparator defined them equal. It uses
 * the [selector] function to transform values and then compares them with the given [comparator].
 */
@kotlin.internal.InlineOnly
public inline fun <T, K> Comparator<T>.thenByDescending(comparator: Comparator<in K>, crossinline selector: (T) -> K): Comparator<T> =
        Comparator { a, b ->
            val previousCompare = this@thenByDescending.compare(a, b)
            if (previousCompare != 0) previousCompare else compareValuesBy(b, a, comparator, selector)
        }


/**
 * Creates a comparator using the primary comparator and function to calculate a result of comparison.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Comparator<T>.thenComparator(crossinline comparison: (a: T, b: T) -> Int): Comparator<T> =
        Comparator { a, b ->
            val previousCompare = this@thenComparator.compare(a, b)
            if (previousCompare != 0) previousCompare else comparison(a, b)
        }

/**
 * Combines this comparator and the given [comparator] such that the latter is applied only
 * when the former considered values equal.
 */
public infix fun <T> Comparator<T>.then(comparator: Comparator<in T>): Comparator<T> =
        Comparator { a, b ->
            val previousCompare = this@then.compare(a, b)
            if (previousCompare != 0) previousCompare else comparator.compare(a, b)
        }

/**
 * Combines this comparator and the given [comparator] such that the latter is applied only
 * when the former considered values equal.
 */
public infix fun <T> Comparator<T>.thenDescending(comparator: Comparator<in T>): Comparator<T> =
        Comparator<T> { a, b ->
            val previousCompare = this@thenDescending.compare(a, b)
            if (previousCompare != 0) previousCompare else comparator.compare(b, a)
        }

// Not so useful without type inference for receiver of expression
/**
 * Extends the given [comparator] of non-nullable values to a comparator of nullable values
 * considering `null` value less than any other value.
 */
public fun <T: Any> nullsFirst(comparator: Comparator<in T>): Comparator<T?> =
        Comparator { a, b ->
            when {
                a === b ->   0
                a == null -> -1
                b == null -> 1
                else -> comparator.compare(a, b)
            }
        }

/**
 * Provides a comparator of nullable [Comparable] values
 * considering `null` value less than any other value.
 */
@kotlin.internal.InlineOnly
public inline fun <T: Comparable<T>> nullsFirst(): Comparator<T?> = nullsFirst(naturalOrder())

/**
 * Extends the given [comparator] of non-nullable values to a comparator of nullable values
 * considering `null` value greater than any other value.
 */
public fun <T: Any> nullsLast(comparator: Comparator<in T>): Comparator<T?> =
        Comparator { a, b ->
            when {
                a === b ->   0
                a == null -> 1
                b == null -> -1
                else -> comparator.compare(a, b)
            }
        }

/**
 * Provides a comparator of nullable [Comparable] values
 * considering `null` value greater than any other value.
 */
@kotlin.internal.InlineOnly
public inline fun <T: Comparable<T>> nullsLast(): Comparator<T?> = nullsLast(naturalOrder())

/**
 * Returns a comparator that compares [Comparable] objects in natural order.
 */
public fun <T: Comparable<T>> naturalOrder(): Comparator<T> = @Suppress("UNCHECKED_CAST") (NaturalOrderComparator as Comparator<T>)

/**
 * Returns a comparator that compares [Comparable] objects in reversed natural order.
 */
public fun <T: Comparable<T>> reverseOrder(): Comparator<T> = @Suppress("UNCHECKED_CAST") (ReverseOrderComparator as Comparator<T>)

/** Returns a comparator that imposes the reverse ordering of this comparator. */
public fun <T> Comparator<T>.reversed(): Comparator<T> = when (this) {
    is ReversedComparator -> this.comparator
    NaturalOrderComparator -> @Suppress("UNCHECKED_CAST") (ReverseOrderComparator as Comparator<T>)
    ReverseOrderComparator -> @Suppress("UNCHECKED_CAST") (NaturalOrderComparator as Comparator<T>)
    else -> ReversedComparator(this)
}


private class ReversedComparator<T>(public val comparator: Comparator<T>): Comparator<T> {
    override fun compare(a: T, b: T): Int = comparator.compare(b, a)
    @Suppress("VIRTUAL_MEMBER_HIDDEN")
    fun reversed(): Comparator<T> = comparator
}

private object NaturalOrderComparator : Comparator<Comparable<Any>> {
    override fun compare(a: Comparable<Any>, b: Comparable<Any>): Int = a.compareTo(b)
    @Suppress("VIRTUAL_MEMBER_HIDDEN")
    fun reversed(): Comparator<Comparable<Any>> = ReverseOrderComparator
}

private object ReverseOrderComparator: Comparator<Comparable<Any>> {
    override fun compare(a: Comparable<Any>, b: Comparable<Any>): Int = b.compareTo(a)
    @Suppress("VIRTUAL_MEMBER_HIDDEN")
    fun reversed(): Comparator<Comparable<Any>> = NaturalOrderComparator
}
