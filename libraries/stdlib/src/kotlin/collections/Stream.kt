package kotlin

import java.util.*

/**
 * A sequence that returns values through its iterator. The values are evaluated lazily, and the sequence
 * is potentially infinite.
 *
 * @param T the type of elements in the sequence.
 */
public trait Stream<out T> {
    /**
     * Returns an iterator that returns the values from the sequence.
     */
    public fun iterator(): Iterator<T>
}

/**
 * Creates a stream that returns the specified values.
 */
public fun <T> streamOf(vararg elements: T): Stream<T> = elements.stream()

/**
 * Creates a stream that returns all values in the specified [progression].
 */
public fun <T> streamOf(progression: Progression<T>): Stream<T> = object : Stream<T> {
    override fun iterator(): Iterator<T> = progression.iterator()
}

/**
 * A stream that returns the values from the underlying [stream] that either match or do not match
 * the specified [predicate].
 *
 * @param sendWhen If `true`, values for which the predicate returns `true` are returned. Otherwise,
 * values for which the predicate returns `false` are returned
 */
public class FilteringStream<T>(private val stream: Stream<T>,
                                private val sendWhen: Boolean = true,
                                private val predicate: (T) -> Boolean
                               ) : Stream<T> {

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val iterator = stream.iterator();
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
 * A stream which returns the results of applying the given [transformer] function to the values
 * in the underlying [stream].
 */
public class TransformingStream<T, R>(private val stream: Stream<T>, private val transformer: (T) -> R) : Stream<R> {
    override fun iterator(): Iterator<R> = object : Iterator<R> {
        val iterator = stream.iterator()
        override fun next(): R {
            return transformer(iterator.next())
        }
        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }
    }
}

/**
 * A stream which returns the results of applying the given [transformer] function to the values
 * in the underlying [stream], where the transformer function takes the index of the value in the underlying
 * stream along with the value itself.
 */
public class TransformingIndexedStream<T, R>(private val stream: Stream<T>, private val transformer: (Int, T) -> R) : Stream<R> {
    override fun iterator(): Iterator<R> = object : Iterator<R> {
        val iterator = stream.iterator()
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
 * A stream which combines values from the underlying [stream] with their indices and returns them as
 * [IndexedValue] objects.
 */
public class IndexingStream<T>(private val stream: Stream<T>) : Stream<IndexedValue<T>> {
    override fun iterator(): Iterator<IndexedValue<T>> = object : Iterator<IndexedValue<T>> {
        val iterator = stream.iterator()
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
 * A stream which takes the values from two parallel underlying streams, passes them to the given
 * [transform] function and returns the values returned by that function. The stream stops returning
 * values as soon as one of the underlying streams stops returning values.
 */
public class MergingStream<T1, T2, V>(private val stream1: Stream<T1>,
                                      private val stream2: Stream<T2>,
                                      private val transform: (T1, T2) -> V
                                     ) : Stream<V> {
    override fun iterator(): Iterator<V> = object : Iterator<V> {
        val iterator1 = stream1.iterator()
        val iterator2 = stream2.iterator()
        override fun next(): V {
            return transform(iterator1.next(), iterator2.next())
        }
        override fun hasNext(): Boolean {
            return iterator1.hasNext() && iterator2.hasNext()
        }
    }
}

public class FlatteningStream<T, R>(private val stream: Stream<T>,
                                    private val transformer: (T) -> Stream<R>
                                   ) : Stream<R> {
    override fun iterator(): Iterator<R> = object : Iterator<R> {
        val iterator = stream.iterator()
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

public class Multistream<T>(private val stream: Stream<Stream<T>>) : Stream<T> {
    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val iterator = stream.iterator()
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
 * A stream that returns at most [count] values from the underlying [stream], and stops returning values
 * as soon as that count is reached.
 */
public class TakeStream<T>(private val stream: Stream<T>,
                           private val count: Int
                          ) : Stream<T> {
    {
        if (count < 0)
            throw IllegalArgumentException("count should be non-negative, but is $count")
    }

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        var left = count
        val iterator = stream.iterator();

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
 * A stream that returns values from the underlying [stream] while the [predicate] function returns
 * `true`, and stops returning values once the function returns `false` for the next element.
 */
public class TakeWhileStream<T>(private val stream: Stream<T>,
                                private val predicate: (T) -> Boolean
                               ) : Stream<T> {
    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val iterator = stream.iterator();
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
 * A stream that skips the specified number of values from the underlying stream and returns
 * all values after that.
 */
public class DropStream<T>(private val stream: Stream<T>,
                           private val count: Int
                          ) : Stream<T> {
    {
        if (count < 0)
            throw IllegalArgumentException("count should be non-negative, but is $count")
    }

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val iterator = stream.iterator();
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
 * A stream that skips the values from the underlying stream while the given [predicate] returns `true` and returns
 * all values after that.
 */
public class DropWhileStream<T>(private val stream: Stream<T>,
                                private val predicate: (T) -> Boolean
                               ) : Stream<T> {

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val iterator = stream.iterator();
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
 * A stream which repeatedly calls the specified [producer] function and returns its return values, until
 * `null` is returned from [producer].
 */
public class FunctionStream<T : Any>(private val producer: () -> T?) : Stream<T> {
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
 * Returns a stream which invokes the function to calculate the next value on each iteration until the function returns `null`.
 */
public fun <T : Any> stream(nextFunction: () -> T?): Stream<T> {
    return FunctionStream(nextFunction)
}

/**
 * Returns a stream which invokes the function to calculate the next value based on the previous one on each iteration
 * until the function returns `null`.
 */
public /*inline*/ fun <T : Any> stream(initialValue: T, nextFunction: (T) -> T?): Stream<T> =
        stream(nextFunction.toGenerator(initialValue))

