package kotlin

import java.util.NoSuchElementException

deprecated("Use Sequence<T> instead.")
public trait Stream<out T> {
    /**
     * Returns an iterator that returns the values from the sequence.
     */
    public fun iterator(): Iterator<T>
}

/**
 * A sequence that returns values through its iterator. The values are evaluated lazily, and the sequence
 * is potentially infinite.
 *
 * @param T the type of elements in the sequence.
 */
public trait Sequence<out T> : Stream<T>

public fun<T> Stream<T>.toSequence(): Sequence<T> = object : Sequence<T> {
    override fun iterator(): Iterator<T> = this@toSequence.iterator()
}

deprecated("Use sequenceOf() instead")
public fun <T> streamOf(vararg elements: T): Stream<T> = elements.stream()

deprecated("Use sequenceOf() instead")
public fun <T> streamOf(progression: Progression<T>): Stream<T> = object : Stream<T> {
    override fun iterator(): Iterator<T> = progression.iterator()
}

/**
 * Creates a sequence that returns the specified values.
 */
public fun <T> sequenceOf(vararg elements: T): Sequence<T> = elements.sequence()

/**
 * Creates a sequence that returns all values in the specified [progression].
 */
public fun <T> sequenceOf(progression: Progression<T>): Sequence<T> = object : Sequence<T> {
    override fun iterator(): Iterator<T> = progression.iterator()
}

deprecated("Use FilteringSequence<T> instead")
public class FilteringStream<T>(stream: Stream<T>, sendWhen: Boolean = true, predicate: (T) -> Boolean)
: Stream<T> by FilteringSequence<T>(stream.toSequence(), sendWhen, predicate)

/**
 * A sequence that returns the values from the underlying [sequence] that either match or do not match
 * the specified [predicate].
 *
 * @param sendWhen If `true`, values for which the predicate returns `true` are returned. Otherwise,
* values for which the predicate returns `false` are returned
 */
public class FilteringSequence<T>(private val sequence: Sequence<T>,
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

deprecated("Use TransformingSequence<T> instead")
public class TransformingStream<T, R>(stream: Stream<T>, transformer: (T) -> R)
: Stream<R> by TransformingSequence<T, R>(stream.toSequence(), transformer)

/**
 * A sequence which returns the results of applying the given [transformer] function to the values
 * in the underlying [sequence].
 */
public class TransformingSequence<T, R>(private val sequence: Sequence<T>, private val transformer: (T) -> R) : Sequence<R> {
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

deprecated("Use TransformingIndexedSequence<T> instead")
public class TransformingIndexedStream<T, R>(stream: Stream<T>, transformer: (Int, T) -> R)
: Stream<R> by TransformingIndexedSequence<T, R>(stream.toSequence(), transformer)

/**
 * A sequence which returns the results of applying the given [transformer] function to the values
 * in the underlying [sequence], where the transformer function takes the index of the value in the underlying
 * sequence along with the value itself.
 */
public class TransformingIndexedSequence<T, R>(private val sequence: Sequence<T>, private val transformer: (Int, T) -> R) : Sequence<R> {
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

deprecated("Use IndexingSequence<T> instead")
public class IndexingStream<T>(stream: Stream<T>)
: Stream<IndexedValue<T>> by IndexingSequence(stream.toSequence())

/**
 * A sequence which combines values from the underlying [sequence] with their indices and returns them as
 * [IndexedValue] objects.
 */
public class IndexingSequence<T>(private val sequence: Sequence<T>) : Sequence<IndexedValue<T>> {
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

deprecated("Use MergingSequence<T> instead")
public class MergingStream<T1, T2, V>(stream1: Stream<T1>, stream2: Stream<T2>, transform: (T1, T2) -> V)
: Stream<V> by MergingSequence(stream1.toSequence(), stream2.toSequence(), transform)

/**
 * A sequence which takes the values from two parallel underlying sequences, passes them to the given
 * [transform] function and returns the values returned by that function. The sequence stops returning
 * values as soon as one of the underlying sequences stops returning values.
 */
public class MergingSequence<T1, T2, V>(private val sequence1: Sequence<T1>,
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

deprecated("Use FlatteningSequence<T> instead")
public class FlatteningStream<T, R>(stream: Stream<T>, transformer: (T) -> Stream<R>)
: Stream<R> by FlatteningSequence(stream.toSequence(), { transformer(it).toSequence() })

public class FlatteningSequence<T, R>(private val sequence: Sequence<T>,
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

deprecated("Use MultiSequence<T> instead")
public class Multistream<T>(stream: Stream<Stream<T>>)
: Stream<T> by FlatteningSequence(stream.toSequence(), { it.toSequence() })

public class MultiSequence<T>(private val sequence: Sequence<Sequence<T>>) : Sequence<T> {
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

deprecated("Use TakeSequence<T> instead")
public class TakeStream<T>(stream: Stream<T>, count: Int)
: Stream<T> by TakeSequence(stream.toSequence(), count)

/**
 * A sequence that returns at most [count] values from the underlying [sequence], and stops returning values
 * as soon as that count is reached.
 */
public class TakeSequence<T>(private val sequence: Sequence<T>,
                             private val count: Int
                            ) : Sequence<T> {
    {
        if (count < 0)
            throw IllegalArgumentException("count should be non-negative, but is $count")
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

deprecated("Use TakeWhileSequence<T> instead")
public class TakeWhileStream<T>(stream: Stream<T>, predicate: (T) -> Boolean) : Stream<T> by TakeWhileSequence<T>(stream.toSequence(), predicate)

/**
 * A sequence that returns values from the underlying [sequence] while the [predicate] function returns
 * `true`, and stops returning values once the function returns `false` for the next element.
 */
public class TakeWhileSequence<T>(private val sequence: Sequence<T>,
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

deprecated("Use DropSequence<T> instead")
public class DropStream<T>(stream: Stream<T>, count: Int)
: Stream<T> by DropSequence<T>(stream.toSequence(), count)

/**
 * A sequence that skips the specified number of values from the underlying [sequence] and returns
 * all values after that.
 */
public class DropSequence<T>(private val sequence: Sequence<T>,
                             private val count: Int
                            ) : Sequence<T> {
    {
        if (count < 0)
            throw IllegalArgumentException("count should be non-negative, but is $count")
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

deprecated("Use DropWhileSequence<T> instead")
public class DropWhileStream<T>(stream: Stream<T>, predicate: (T) -> Boolean) : Stream<T> by DropWhileSequence<T>(stream.toSequence(), predicate)

/**
 * A sequence that skips the values from the underlying [sequence] while the given [predicate] returns `true` and returns
 * all values after that.
 */
public class DropWhileSequence<T>(private val sequence: Sequence<T>,
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

/**
 * A sequence which repeatedly calls the specified [producer] function and returns its return values, until
 * `null` is returned from [producer].
 */
public class FunctionSequence<T : Any>(private val producer: () -> T?) : Sequence<T> {
    override fun iterator(): Iterator<T> = object : Iterator<T> {
        var nextState: Int = -1 // -1 for unknown, 0 for done, 1 for continue
        var nextItem: T? = null

        private fun calcNext() {
            val item = producer()
            if (item == null) {
                nextState = 0
            } else {
                nextState = 1
                nextItem = item
            }
        }

        override fun next(): T {
            if (nextState == -1)
                calcNext()
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
                calcNext()
            return nextState == 1
        }

    }
}

/**
 * Returns a sequence which invokes the function to calculate the next value on each iteration until the function returns `null`.
 */
public fun <T : Any> sequence(nextFunction: () -> T?): Sequence<T> {
    return FunctionSequence(nextFunction)
}

deprecated("Use sequence() instead")
public fun <T : Any> stream(nextFunction: () -> T?): Sequence<T> = sequence(nextFunction)

/**
 * Returns a sequence which invokes the function to calculate the next value based on the previous one on each iteration
 * until the function returns `null`.
 */
public /*inline*/ fun <T : Any> sequence(initialValue: T, nextFunction: (T) -> T?): Sequence<T> =
        sequence(nextFunction.toGenerator(initialValue))

deprecated("Use sequence() instead")
public /*inline*/ fun <T : Any> stream(initialValue: T, nextFunction: (T) -> T?): Sequence<T> = sequence(initialValue, nextFunction)
