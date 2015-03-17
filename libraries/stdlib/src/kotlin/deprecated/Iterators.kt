package kotlin

import kotlin.support.*
import java.util.Collections
import kotlin.test.assertTrue

/**
 * Returns an iterator which invokes the function to calculate the next value on each iteration until the function returns *null*
 */
deprecated("Use sequence(...) function to make lazy sequence of values.")
public fun <T:Any> iterate(nextFunction: () -> T?) : Iterator<T> {
    return FunctionIterator(nextFunction)
}

/**
 * Returns an iterator which invokes the function to calculate the next value based on the previous one on each iteration
 * until the function returns *null*
 */
deprecated("Use sequence(...) function to make lazy sequence of values.")
public /*inline*/ fun <T: Any> iterate(initialValue: T, nextFunction: (T) -> T?): Iterator<T> =
        iterate(nextFunction.toGenerator(initialValue))

/**
 * Returns an iterator whose values are pairs composed of values produced by given pair of iterators
 */
deprecated("Replace Iterator<T> with Sequence<T> by using sequence() function instead of iterator()")
public fun <T, S> Iterator<T>.zip(iterator: Iterator<S>): Iterator<Pair<T, S>> = PairIterator(this, iterator)

/**
 * Returns an iterator shifted to right by the given number of elements
 */
deprecated("Replace Iterator<T> with Sequence<T> by using sequence() function instead of iterator()")
public fun <T> Iterator<T>.skip(n: Int): Iterator<T> = SkippingIterator(this, n)

deprecated("Use FilteringStream<T> instead")
public class FilterIterator<T>(private val iterator: Iterator<T>, private val predicate: (T) -> Boolean) :
        AbstractIterator<T>() {
    override protected fun computeNext(): Unit {
        while (iterator.hasNext()) {
            val next = iterator.next()
            if ((predicate)(next)) {
                setNext(next)
                return
            }
        }
        done()
    }
}

deprecated("Use FilteringStream<T> instead")
public class FilterNotNullIterator<T : Any>(private val iterator: Iterator<T?>?) : AbstractIterator<T>() {
    override protected fun computeNext(): Unit {
        if (iterator != null) {
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next != null) {
                    setNext(next)
                    return
                }
            }
        }
        done()
    }
}

deprecated("Use TransformingStream<T> instead")
public class MapIterator<T, R>(private val iterator: Iterator<T>, private val transform: (T) -> R) :
        AbstractIterator<R>() {
    override protected fun computeNext(): Unit {
        if (iterator.hasNext()) {
            setNext((transform)(iterator.next()))
        } else {
            done()
        }
    }
}

deprecated("Use FlatteningStream<T> instead")
public class FlatMapIterator<T, R>(private val iterator: Iterator<T>, private val transform: (T) -> Iterator<R>) :
        AbstractIterator<R>() {
    private var transformed: Iterator<R> = iterate<R> { null }

    override protected fun computeNext(): Unit {
        while (true) {
            if (transformed.hasNext()) {
                setNext(transformed.next())
                return
            }
            if (iterator.hasNext()) {
                transformed = (transform)(iterator.next())
            } else {
                done()
                return
            }
        }
    }
}

deprecated("Use LimitedStream<T> instead")
public class TakeWhileIterator<T>(private val iterator: Iterator<T>, private val predicate: (T) -> Boolean) :
        AbstractIterator<T>() {
    override protected fun computeNext() : Unit {
        if (iterator.hasNext()) {
            val item = iterator.next()
            if ((predicate)(item)) {
                setNext(item)
                return
            }
        }
        done()
    }
}

/** An [[Iterator]] which invokes a function to calculate the next value in the iteration until the function returns *null* */
deprecated("Use FunctionStream<T> instead")
public class FunctionIterator<T : Any>(private val nextFunction: () -> T?) : AbstractIterator<T>() {

    override protected fun computeNext(): Unit {
        val next = (nextFunction)()
        if (next == null) {
            done()
        } else {
            setNext(next)
        }
    }
}

/** An [[Iterator]] which iterates over a number of iterators in sequence */
deprecated("Use Multistream<T> instead")
public fun CompositeIterator<T>(vararg iterators: Iterator<T>): CompositeIterator<T> = CompositeIterator(iterators.iterator())

deprecated("Use Multistream<T> instead")
public class CompositeIterator<T>(private val iterators: Iterator<Iterator<T>>) : AbstractIterator<T>() {

    private var currentIter: Iterator<T>? = null

    override protected fun computeNext(): Unit {
        while (true) {
            if (currentIter == null) {
                if (iterators.hasNext()) {
                    currentIter = iterators.next()
                } else {
                    done()
                    return
                }
            }
            val iter = currentIter
            if (iter != null) {
                if (iter.hasNext()) {
                    setNext(iter.next())
                    return
                } else {
                    currentIter = null
                }
            }
        }
    }
}

/** A singleton [[Iterator]] which invokes once over a value */
deprecated("Use streams for lazy collection operations.")
public class SingleIterator<T>(private val value: T) : AbstractIterator<T>() {
    private var first = true

    override protected fun computeNext(): Unit {
        if (first) {
            first = false
            setNext(value)
        } else {
            done()
        }
    }
}

deprecated("Use streams for lazy collection operations.")
public class IndexIterator<T>(private val iterator: Iterator<T>) : Iterator<Pair<Int, T>> {
    private var index: Int = 0

    override fun next(): Pair<Int, T> {
        return Pair(index++, iterator.next())
    }

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }
}

deprecated("Use ZippingStream<T> instead.")
public class PairIterator<T, S>(
        private val iterator1: Iterator<T>, private val iterator2: Iterator<S>
): AbstractIterator<Pair<T, S>>() {
    protected override fun computeNext() {
        if (iterator1.hasNext() && iterator2.hasNext()) {
            setNext(Pair(iterator1.next(), iterator2.next()))
        }
        else {
            done()
        }
    }
}

deprecated("Use streams for lazy collection operations.")
public class SkippingIterator<T>(private val iterator: Iterator<T>, private val n: Int) : Iterator<T> {
    private var firstTime: Boolean = true

    private fun skip() {
        for (i in 1..n) {
            if (!iterator.hasNext()) break
            iterator.next()
        }
        firstTime = false
    }

    override fun next(): T {
        assertTrue(!firstTime, "hasNext() must be invoked before advancing an iterator")
        return iterator.next()
    }

    override fun hasNext(): Boolean {
        if (firstTime) {
            skip()
        }
        return iterator.hasNext()
    }
}
