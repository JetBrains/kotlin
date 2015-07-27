package kotlin

import java.util.*
import kotlin.support.AbstractIterator

/**
 * A sequence that returns values through its iterator. The values are evaluated lazily, and the sequence
 * is potentially infinite.
 *
 * @param T the type of elements in the sequence.
 */
public interface Sequence<out T> {
    /**
     * Returns an iterator that returns the values from the sequence.
     */
    public fun iterator(): Iterator<T>
}

/**
 * Creates a sequence that returns all values from this iterator. The sequence is constrained to be iterated only once.
 */
public fun <T> Iterator<T>.asSequence(): Sequence<T> {
    val iteratorSequence = object : Sequence<T> {
        override fun iterator(): Iterator<T> = this@asSequence
    }
    return iteratorSequence.constrainOnce()
}

deprecated("Use asSequence() instead.", ReplaceWith("asSequence()"))
public fun <T> Iterator<T>.sequence(): Sequence<T> = asSequence()


/**
 * Creates a sequence that returns all values from this enumeration. The sequence is constrained to be iterated only once.
 */
public fun<T> Enumeration<T>.asSequence(): Sequence<T> = this.iterator().asSequence()

/**
 * Creates a sequence that returns the specified values.
 */
public fun <T> sequenceOf(vararg elements: T): Sequence<T> = if (elements.isEmpty()) emptySequence() else elements.asSequence()

/**
 * Creates a sequence that returns all values in the specified [progression].
 */
public fun <T> sequenceOf(progression: Progression<T>): Sequence<T> = object : Sequence<T> {
    override fun iterator(): Iterator<T> = progression.iterator()
}

/**
 * Returns an [Iterable] instance that wraps the original sequence.
 */
public fun <T> Sequence<T>.asIterable(): Iterable<T> {
    return object : Iterable<T> {
        override fun iterator(): Iterator<T> = this@asIterable.iterator()
    }
}

/**
 * Returns an empty sequence.
 */
public fun <T> emptySequence(): Sequence<T> = EmptySequence

private object EmptySequence : Sequence<Nothing> {
    override fun iterator(): Iterator<Nothing> = EmptyIterator
}

/**
 * A sequence that returns the values from the underlying [sequence] that either match or do not match
 * the specified [predicate].
 *
 * @param sendWhen If `true`, values for which the predicate returns `true` are returned. Otherwise,
* values for which the predicate returns `false` are returned
 */
private class FilteringSequence<T>(private val sequence: Sequence<T>,
                                  private val sendWhen: Boolean = true,
                                  private val predicate: (T) -> Boolean
                                 ) : Sequence<T> {

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val iterator = sequence.iterator();
        var nextState: Int = -1 // -1 for unknown, 0 for done, 1 for continue
        var nextItem: T? = null

        private fun calcNext() {
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (predicate(item) == sendWhen) {
                    nextItem = item
                    nextState = 1
                    return
                }
            }
            nextState = 0
        }

        override fun next(): T {
            if (nextState == -1)
                calcNext()
            if (nextState == 0)
                throw NoSuchElementException()
            val result = nextItem
            nextItem = null
            nextState = -1
            return result as T
        }

        override fun hasNext(): Boolean {
            if (nextState == -1)
                calcNext()
            return nextState == 1
        }
    }
}

/**
 * A sequence which returns the results of applying the given [transformer] function to the values
 * in the underlying [sequence].
 */

private class TransformingSequence<T, R>
constructor(private val sequence: Sequence<T>, private val transformer: (T) -> R) : Sequence<R> {
    override fun iterator(): Iterator<R> = object : Iterator<R> {
        val iterator = sequence.iterator()
        override fun next(): R {
            return transformer(iterator.next())
        }

        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }
    }
}

/**
 * A sequence which returns the results of applying the given [transformer] function to the values
 * in the underlying [sequence], where the transformer function takes the index of the value in the underlying
 * sequence along with the value itself.
 */
private class TransformingIndexedSequence<T, R>
constructor(private val sequence: Sequence<T>, private val transformer: (Int, T) -> R) : Sequence<R> {
    override fun iterator(): Iterator<R> = object : Iterator<R> {
        val iterator = sequence.iterator()
        var index = 0
        override fun next(): R {
            return transformer(index++, iterator.next())
        }

        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }
    }
}

/**
 * A sequence which combines values from the underlying [sequence] with their indices and returns them as
 * [IndexedValue] objects.
 */
private class IndexingSequence<T>
constructor(private val sequence: Sequence<T>) : Sequence<IndexedValue<T>> {
    override fun iterator(): Iterator<IndexedValue<T>> = object : Iterator<IndexedValue<T>> {
        val iterator = sequence.iterator()
        var index = 0
        override fun next(): IndexedValue<T> {
            return IndexedValue(index++, iterator.next())
        }

        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }
    }
}

/**
 * A sequence which takes the values from two parallel underlying sequences, passes them to the given
 * [transform] function and returns the values returned by that function. The sequence stops returning
 * values as soon as one of the underlying sequences stops returning values.
 */
private class MergingSequence<T1, T2, V>
                                       constructor(private val sequence1: Sequence<T1>,
                                        private val sequence2: Sequence<T2>,
                                        private val transform: (T1, T2) -> V
                                       ) : Sequence<V> {
    override fun iterator(): Iterator<V> = object : Iterator<V> {
        val iterator1 = sequence1.iterator()
        val iterator2 = sequence2.iterator()
        override fun next(): V {
            return transform(iterator1.next(), iterator2.next())
        }

        override fun hasNext(): Boolean {
            return iterator1.hasNext() && iterator2.hasNext()
        }
    }
}

private class FlatteningSequence<T, R>
                                     constructor(private val sequence: Sequence<T>,
                                      private val transformer: (T) -> Sequence<R>
                                     ) : Sequence<R> {
    override fun iterator(): Iterator<R> = object : Iterator<R> {
        val iterator = sequence.iterator()
        var itemIterator: Iterator<R>? = null

        override fun next(): R {
            if (!ensureItemIterator())
                throw NoSuchElementException()
            return itemIterator!!.next()
        }

        override fun hasNext(): Boolean {
            return ensureItemIterator()
        }

        private fun ensureItemIterator(): Boolean {
            if (itemIterator?.hasNext() == false)
                itemIterator = null

            while (itemIterator == null) {
                if (!iterator.hasNext()) {
                    return false
                } else {
                    val element = iterator.next()
                    val nextItemIterator = transformer(element).iterator()
                    if (nextItemIterator.hasNext()) {
                        itemIterator = nextItemIterator
                        return true
                    }
                }
            }
            return true
        }
    }
}

private class MultiSequence<T>
constructor(private val sequence: Sequence<Sequence<T>>) : Sequence<T> {
    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val iterator = sequence.iterator()
        var itemIterator: Iterator<T>? = null

        override fun next(): T {
            if (!ensureItemIterator())
                throw NoSuchElementException()
            return itemIterator!!.next()
        }

        override fun hasNext(): Boolean {
            return ensureItemIterator()
        }

        private fun ensureItemIterator(): Boolean {
            if (itemIterator?.hasNext() == false)
                itemIterator = null

            while (itemIterator == null) {
                if (!iterator.hasNext()) {
                    return false
                } else {
                    val element = iterator.next()
                    val nextItemIterator = element.iterator()
                    if (nextItemIterator.hasNext()) {
                        itemIterator = nextItemIterator
                        return true
                    }
                }
            }
            return true
        }
    }
}

/**
 * A sequence that returns at most [count] values from the underlying [sequence], and stops returning values
 * as soon as that count is reached.
 */
private class TakeSequence<T>
                            constructor(private val sequence: Sequence<T>,
                             private val count: Int
                            ) : Sequence<T> {
    init {
        require (count >= 0) { throw IllegalArgumentException("count should be non-negative, but is $count") }
    }

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        var left = count
        val iterator = sequence.iterator();

        override fun next(): T {
            if (left == 0)
                throw NoSuchElementException()
            left--
            return iterator.next()
        }

        override fun hasNext(): Boolean {
            return left > 0 && iterator.hasNext()
        }
    }
}

/**
 * A sequence that returns values from the underlying [sequence] while the [predicate] function returns
 * `true`, and stops returning values once the function returns `false` for the next element.
 */
private class TakeWhileSequence<T>
                                 constructor(private val sequence: Sequence<T>,
                                  private val predicate: (T) -> Boolean
                                 ) : Sequence<T> {
    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val iterator = sequence.iterator();
        var nextState: Int = -1 // -1 for unknown, 0 for done, 1 for continue
        var nextItem: T? = null

        private fun calcNext() {
            if (iterator.hasNext()) {
                val item = iterator.next()
                if (predicate(item)) {
                    nextState = 1
                    nextItem = item
                    return
                }
            }
            nextState = 0
        }

        override fun next(): T {
            if (nextState == -1)
                calcNext() // will change nextState
            if (nextState == 0)
                throw NoSuchElementException()
            val result = nextItem as T

            // Clean next to avoid keeping reference on yielded instance
            nextItem = null
            nextState = -1
            return result
        }

        override fun hasNext(): Boolean {
            if (nextState == -1)
                calcNext() // will change nextState
            return nextState == 1
        }
    }
}

/**
 * A sequence that skips the specified number of values from the underlying [sequence] and returns
 * all values after that.
 */
private class DropSequence<T>
                            constructor(private val sequence: Sequence<T>,
                             private val count: Int
                            ) : Sequence<T> {
    init {
        require (count >= 0) { throw IllegalArgumentException("count should be non-negative, but is $count") }
    }

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val iterator = sequence.iterator();
        var left = count

        // Shouldn't be called from constructor to avoid premature iteration
        private fun drop() {
            while (left > 0 && iterator.hasNext()) {
                iterator.next()
                left--
            }
        }

        override fun next(): T {
            drop()
            return iterator.next()
        }

        override fun hasNext(): Boolean {
            drop()
            return iterator.hasNext()
        }
    }
}

/**
 * A sequence that skips the values from the underlying [sequence] while the given [predicate] returns `true` and returns
 * all values after that.
 */
private class DropWhileSequence<T>
                                 constructor(private val sequence: Sequence<T>,
                                  private val predicate: (T) -> Boolean
                                 ) : Sequence<T> {

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val iterator = sequence.iterator();
        var dropState: Int = -1 // -1 for not dropping, 1 for nextItem, 0 for normal iteration
        var nextItem: T? = null

        private fun drop() {
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (!predicate(item)) {
                    nextItem = item
                    dropState = 1
                    return
                }
            }
            dropState = 0
        }

        override fun next(): T {
            if (dropState == -1)
                drop()

            if (dropState == 1) {
                val result = nextItem as T
                nextItem = null
                dropState = 0
                return result
            }
            return iterator.next()
        }

        override fun hasNext(): Boolean {
            if (dropState == -1)
                drop()
            return dropState == 1 || iterator.hasNext()
        }
    }
}

private class DistinctSequence<T, K>(private val source : Sequence<T>, private val keySelector : (T) -> K) : Sequence<T> {
    override fun iterator(): Iterator<T> = DistinctIterator(source.iterator(), keySelector)
}

private class DistinctIterator<T, K>(private val source : Iterator<T>, private val keySelector : (T) -> K) : AbstractIterator<T>() {
    private val observed = HashSet<K>()

    override fun computeNext() {
        while (source.hasNext()) {
            val next = source.next()
            val key = keySelector(next)

            if (observed.add(key)) {
                setNext(next)
                return
            }
        }

        done()
    }
}


private class GeneratorSequence<T: Any>(private val getInitialValue: () -> T?, private val getNextValue: (T) -> T?): Sequence<T> {
    override fun iterator(): Iterator<T> = object : Iterator<T> {
        var nextItem: T? = getInitialValue()
        var nextState: Int = if (nextItem == null) 0 else 1 // -1 for unknown, 0 for done, 1 for continue

        private fun calcNext() {
            nextItem = getNextValue(nextItem!!)
            nextState = if (nextItem == null) 0 else 1
        }

        override fun next(): T {
            if (nextState == -1)
                calcNext()
            if (nextState == 0)
                throw NoSuchElementException()
            val result = nextItem as T
            // Clean next to avoid keeping reference on yielded instance
            // need to keep state
            // nextItem = null
            nextState = -1
            return result
        }

        override fun hasNext(): Boolean {
            if (nextState == -1)
                calcNext()
            return nextState == 1
        }
    }
}

/**
 * Returns a wrapper sequence that provides values of this sequence, but ensures it can be iterated only one time.
 *
 * [IllegalStateException] is thrown on iterating the returned sequence from the second time.
 */
public fun <T> Sequence<T>.constrainOnce(): Sequence<T> {
    // as? does not work in js
    //return this as? ConstrainedOnceSequence<T> ?: ConstrainedOnceSequence(this)
    return if (this is ConstrainedOnceSequence<T>) this else ConstrainedOnceSequence(this)
}

/**
 * Returns a sequence which invokes the function to calculate the next value on each iteration until the function returns `null`.
 *
 * Returned sequence is constrained to be iterated only once.
 *
 * @see constrainOnce
 */
public fun <T : Any> sequence(nextFunction: () -> T?): Sequence<T> {
    return GeneratorSequence(nextFunction, { nextFunction() }).constrainOnce()
}

/**
 * Returns a sequence which invokes the function to calculate the next value based on the previous one on each iteration
 * until the function returns `null`. The sequence starts with the specified [initialValue].
 *
 * The sequence can be iterated multiple times, each time starting with the [initialValue].
 */
public /*inline*/ fun <T : Any> sequence(initialValue: T, nextFunction: (T) -> T?): Sequence<T> =
        GeneratorSequence({ initialValue }, nextFunction)

/**
 * Returns a sequence which invokes the function [initialValueFunction] to get the first item and then
 * [nextFunction] to calculate the next value based on the previous one on each iteration
 * until the function returns `null`. The sequence starts with the value returned by [initialValueFunction].
 */
public fun <T: Any> sequence(initialValueFunction: () -> T?, nextFunction: (T) -> T?): Sequence<T> =
        GeneratorSequence(initialValueFunction, nextFunction)
