package kotlin

import kotlin.support.*
import java.util.Collections
import kotlin.test.assertTrue

/**
 * Returns an iterator which invokes the function to calculate the next value on each iteration until the function returns *null*
 */
public inline fun <T:Any> iterate(nextFunction: () -> T?) : Iterator<T> {
    return FunctionIterator(nextFunction)
}

/**
 * Returns an iterator which invokes the function to calculate the next value based on the previous one on each iteration
 * until the function returns *null*
 */
public inline fun <T: Any> iterate(initialValue: T, nextFunction: (T) -> T?): Iterator<T> =
        iterate(nextFunction.toGenerator(initialValue))

/**
 * Returns an iterator whose values are pairs composed of values produced by given pair of iterators
 */
public inline fun <T, S> Iterator<T>.zip(iterator: Iterator<S>): Iterator<Pair<T, S>> = PairIterator(this, iterator)

/**
 * Returns an iterator shifted to right by the given number of elements
 */
public inline fun <T> Iterator<T>.skip(n: Int): Iterator<T> = SkippingIterator(this, n)

class FilterIterator<T>(val iterator : Iterator<T>, val predicate: (T)-> Boolean) : AbstractIterator<T>() {
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

class FilterNotNullIterator<T:Any>(val iterator : Iterator<T?>?) : AbstractIterator<T>() {
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

class MapIterator<T, R>(val iterator : Iterator<T>, val transform: (T) -> R) : AbstractIterator<R>() {
    override protected fun computeNext() : Unit {
        if (iterator.hasNext()) {
            setNext((transform)(iterator.next()))
        } else {
            done()
        }
    }
}

class FlatMapIterator<T, R>(val iterator : Iterator<T>, val transform: (T) -> Iterator<R>) : AbstractIterator<R>() {
    var transformed: Iterator<R> = iterate<R> { null }

    override protected fun computeNext() : Unit {
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

class TakeWhileIterator<T>(val iterator: Iterator<T>, val predicate: (T) -> Boolean) : AbstractIterator<T>() {
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
class FunctionIterator<T:Any>(val nextFunction: () -> T?): AbstractIterator<T>() {

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
class CompositeIterator<T>(vararg iterators: Iterator<T>): AbstractIterator<T>() {

    val iteratorsIter = iterators.iterator()
    var currentIter: Iterator<T>? = null

    override protected fun computeNext(): Unit {
        while (true) {
            if (currentIter == null) {
                if (iteratorsIter.hasNext()) {
                    currentIter = iteratorsIter.next()
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
class SingleIterator<T>(val value: T): AbstractIterator<T>() {
    var first = true

    override protected fun computeNext(): Unit {
        if (first) {
            first = false
            setNext(value)
        } else {
            done()
        }
    }
}

class IndexIterator<T>(val iterator : Iterator<T>): Iterator<Pair<Int, T>> {
    private var index : Int = 0

    override fun next(): Pair<Int, T> {
        return Pair(index++, iterator.next())
    }

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }
}

public class PairIterator<T, S>(
        val iterator1 : Iterator<T>, val iterator2 : Iterator<S>
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

class SkippingIterator<T>(val iterator: Iterator<T>, val n: Int): Iterator<T> {
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

fun <T: Any> Function1<T, T?>.toGenerator(initialValue: T): Function0<T?> {
    var nextValue: T? = initialValue
    return {
        nextValue?.let { result ->
            nextValue = this@toGenerator(result)
            result
        }
    }
}