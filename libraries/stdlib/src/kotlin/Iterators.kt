package kotlin

import kotlin.support.*
import java.util.Collections

/**
 * Returns an iterator which invokes the function to calculate the next value on each iteration until the function returns *null*
 */
public inline fun <T:Any> iterate(nextFunction: () -> T?) : Iterator<T> {
    return FunctionIterator(nextFunction)
}

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
