package kotlin2

import java.util.NoSuchElementException

// not using an enum for now as JS generation doesn't support it
object State {
    val Ready = 0
    val NotReady = 1
    val Done = 2
    val Failed = 3
}

private class FilterIterator<T>(val iterator: Iterator<T>, val predicate: (T)-> Boolean): AbstractIterator<T>() {
    override protected fun computeNext(): Unit {
        while (iterator.hasNext) {
            val next = iterator.next()
            if ((predicate)(next)) {
                setNext(next)
                return
            }
        }
        done()
    }
}

private class FilterNotNullIterator<T>(val iterator: Iterator<T?>?): AbstractIterator<T>() {
    override protected fun computeNext(): Unit {
        if (iterator != null) {
            while (iterator.hasNext) {
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

private class MapIterator<T, R>(val iterator: Iterator<T>, val transform: (T) -> R): AbstractIterator<R>() {
    override protected fun computeNext(): Unit {
        if (iterator.hasNext) {
            setNext((transform)(iterator.next()))
        } else {
            done()
        }
    }
}

private class FlatMapIterator<T, R>(val iterator: Iterator<T>, val transform: (T) -> Iterator<R>): AbstractIterator<R>() {
    var transformed: Iterator<R> = iterate2<R> { null }

    override protected fun computeNext(): Unit {
        while (true) {
            if (transformed.hasNext) {
                setNext(transformed.next())
                return
            }
            if (iterator.hasNext) {
                transformed = (transform)(iterator.next())
            } else {
                done()
                return
            }
        }
    }
}

private class TakeWhileIterator<T>(val iterator: Iterator<T>, val predicate: (T) -> Boolean): AbstractIterator<T>() {
    override protected fun computeNext(): Unit {
        if (iterator.hasNext) {
            val item = iterator.next()
            if ((predicate)(item)) {
                setNext(item)
                return
            }
        }
        done()
    }
}


/**
 * A base class to simplify implementing iterators so that implementations only have to implement [[computeNext()]]
 * to implement the iterator, calling [[done()]] when the iteration is complete.
 */
public abstract class AbstractIterator<T>: Iterator<T> {
    private var state = State.NotReady
    private var next: T? = null

    override val hasNext: Boolean
        get() {
            require(state != State.Failed)
            return when (state) {
                State.Done -> false
                State.Ready -> true
                else -> tryToComputeNext()
            }
        }

    override fun next(): T {
        if (!hasNext) throw NoSuchElementException()
        state = State.NotReady
        return next!!
    }


    /** Returns the next element in the iteration without advancing the iteration */
    fun peek(): T {
        if (!hasNext) throw NoSuchElementException()
        return next!!;
    }

    private fun tryToComputeNext(): Boolean {
        state = State.Failed
        computeNext();
        return state == State.Ready
    }

    /**
     * Computes the next item in the iterator.
     *
     * This callback method should call one of these two methods
     *
     * * [[setNext(T)]] with the next value of the iteration
     * * [[done()]] to indicate there are no more elements
     *
     * Failure to call either method will result in the iteration terminating with a failed state
     */
    abstract protected fun computeNext(): Unit

    /**
     * Sets the next value in the iteration, called from the [[computeNext()]] function
     */
    protected fun setNext(value: T): Unit {
        next = value
        state = State.Ready
    }

    /**
     * Sets the state to done so that the iteration terminates
     */
    protected fun done() {
        state = State.Done
    }
}

/** An [[Iterator]] which invokes a function to calculate the next value in the iteration until the function returns *null* */
class FunctionIterator<T>(val nextFunction: () -> T?): AbstractIterator<T>() {

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
fun CompositeIterator<T>(vararg iterators: Iterator<T>) = CompositeIterator(iterators.iterator())

class CompositeIterator<T>(val iterators: Iterator<Iterator<T>>): AbstractIterator<T>() {

    var currentIter: Iterator<T>? = null

    override protected fun computeNext(): Unit {
        while (true) {
            if (currentIter == null) {
                if (iteratorsIter.hasNext) {
                    currentIter = iteratorsIter.next()
                } else {
                    done()
                    return
                }
            }
            val iter = currentIter
            if (iter != null) {
                if (iter.hasNext) {
                    setNext(iter.next()!!)
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

private fun <T> countTo(n: Int): (T) -> Boolean {
    var count = 0
    return { ++count; count <= n }
}

// TODO called iterate2 for now to avoid clash with kotlin method
public inline fun <T> iterate2(nextFunction: () -> T?) : Iterator<T> = FunctionIterator(nextFunction)
