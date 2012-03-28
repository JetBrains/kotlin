package kotlin

import kotlin.support.AbstractIterator
import kotlin.support.FunctionIterator

/**
 * Returns an iterator which invokes the function to calculate the next value on each iteration until the function returns *null*
 *
 * @includeFunction ../../test/iterators/IteratorsTest.kt fibonacci
 */
inline fun <T> iterate(nextFunction: () -> T?) : java.util.Iterator<T> = FunctionIterator(nextFunction)

/** Returns an iterator over elements that are instances of a given type *R* which is a subclass of *T* */
inline fun <T, R: T> java.util.Iterator<T>.filterIsInstance(klass: Class<R>): java.util.Iterator<R> = FilterIsIterator<T,R>(this, klass)

private class FilterIsIterator<T, R :T>(val iterator : java.util.Iterator<T>, val klass: Class<R>) : AbstractIterator<R>() {
    override protected fun computeNext(): R? {
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (klass.isInstance(next)) return next as R
        }
        done()
        return null
    }
}

/**
 * Returns an iterator over elements which match the given *predicate*
 *
 * @includeFunction ../../test/iterators/IteratorsTest.kt filterAndTakeWhileExtractTheElementsWithinRange
 */
inline fun <T> java.util.Iterator<T>.filter(predicate: (T) -> Boolean) : java.util.Iterator<T> = FilterIterator<T>(this, predicate)

private class FilterIterator<T>(val iterator : java.util.Iterator<T>, val predicate: (T)-> Boolean) : AbstractIterator<T>() {
    override protected fun computeNext(): T? {
        while (iterator.hasNext()) {
            val next = iterator.next()
            if ((predicate)(next)) return next
        }
        done()
        return null
    }
}

/** Returns an iterator over elements which do not match the given *predicate* */
inline fun <T> java.util.Iterator<T>.filterNot(predicate: (T) -> Boolean) : java.util.Iterator<T> = filter { !predicate(it) }

/** Returns an iterator over non-*null* elements */
inline fun <T> java.util.Iterator<T?>?.filterNotNull() : java.util.Iterator<T> = FilterNotNullIterator(this)

private class FilterNotNullIterator<T>(val iterator : java.util.Iterator<T?>?) : AbstractIterator<T>() {
    override protected fun computeNext(): T? {
        if (iterator != null) {
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next != null) return next
            }
        }
        done()
        return null
    }
}

/**
 * Returns an iterator obtained by applying *transform*, a function transforming an object of type *T* into an object of type *R*
 *
 * @includeFunction ../../test/iterators/IteratorsTest.kt mapAndTakeWhileExtractTheTransformedElements
 */
inline fun <T, R> java.util.Iterator<T>.map(transform: (T) -> R): java.util.Iterator<R> = MapIterator<T, R>(this, transform)

private class MapIterator<T, R>(val iterator : java.util.Iterator<T>, val transform: (T) -> R) : AbstractIterator<R>() {
    override protected fun computeNext() : R? = if (iterator.hasNext()) (transform)(iterator.next()) else { done(); null }
}

/**
 * Returns an iterator over the concatenated results of transforming each element to one or more values
 *
 * @includeFunction ../../test/iterators/IteratorsTest.kt flatMapAndTakeExtractTheTransformedElements
 */
inline fun <T, R> java.util.Iterator<T>.flatMap(transform: (T) -> java.util.Iterator<R>): java.util.Iterator<R> = FlatMapIterator<T, R>(this, transform)

private class FlatMapIterator<T, R>(val iterator : java.util.Iterator<T>, val transform: (T) -> java.util.Iterator<R>) : AbstractIterator<R>() {
    var transformed: java.util.Iterator<R> = iterate<R> { null }

    override protected fun computeNext() : R? {
        if (transformed.hasNext()) return transformed.next()
        if (iterator.hasNext()) {
            transformed = (transform)(iterator.next())
            return computeNext()
        }
        done()
        return null
    }
}

/**
 * Returns an iterator restricted to the first *n* elements
 *
 * @includeFunction ../../test/iterators/IteratorsTest.kt takeExtractsTheFirstNElements
 */
inline fun <T> java.util.Iterator<T>.take(n: Int): java.util.Iterator<T> {
    fun countTo(n: Int): (T) -> Boolean {
        var count = 0
        return { ++count; count <= n }
    }
    return takeWhile(countTo(n))
}

/**
 * Returns an iterator restricted to the first elements that match the given *predicate*
 *
 * @includeFunction ../../test/iterators/IteratorsTest.kt filterAndTakeWhileExtractTheElementsWithinRange
 */
inline fun <T> java.util.Iterator<T>.takeWhile(predicate: (T) -> Boolean): java.util.Iterator<T> = TakeWhileIterator<T>(this, predicate)

private class TakeWhileIterator<T>(val iterator: java.util.Iterator<T>, val predicate: (T) -> Boolean) : AbstractIterator<T>() {
    override protected fun computeNext() : T? {
        if (iterator.hasNext()) {
            val item = iterator.next()
            if ((predicate)(item)) return item
        }
        done()
        return null
    }
}

/**
 * Creates a string from the first *n (= limit)* elements separated using the *separator* and using the given *prefix* and *postfix* if supplied
 *
 * @includeFunction ../../test/iterators/IteratorsTest.kt joinConcatenatesTheFirstNElementsAboveAThreshold
 */
inline fun <T> java.util.Iterator<T>.join(separator: String, prefix: String = "", postfix: String = "", limit: Int = 20) : String {
    val buffer = StringBuilder(prefix)
    var first = true; var count = 0
    for (element in this) {
        if (first) first = false else buffer.append(separator)
        if (++count <= limit) buffer.append(element) else break
    }
    if (count > limit) buffer.append("...")
    return buffer.append(postfix).toString().sure()
}

/**
 * Returns a comma-separated representation of no more than the first 10 elements
 *
 * @includeFunction ../../test/iterators/IteratorsTest.kt toStringJoinsNoMoreThanTheFirstTenElements
 */
inline fun <T> java.util.Iterator<T>.toString(): String = join(separator = ", ", limit = 10)
