package kotlin.support

import java.util.NoSuchElementException

enum class State {
    Ready
    NotReady
    Done
    Failed
}

/**
 * A base class to simplify implementing iterators so that implementations only have to implement [[computeNext()]]
 * to implement the iterator, calling [[done()]] when the iteration is complete.
 */
public abstract class AbstractIterator<T>: java.util.Iterator<T> {
    private var state: State = State.NotReady
    private var next: T? = null

    override fun hasNext(): Boolean {
        require(state != State.Failed)
        return when (state) {
            State.Done -> false
            State.Ready -> true
            else -> tryToComputeNext()
        }
    }

    override fun next(): T {
        if (!hasNext()) throw NoSuchElementException()
        state = State.NotReady
        return next.sure()
    }

    override fun remove() {
        throw UnsupportedOperationException()
    }

    /** Returns the next element in the iteration without advancing the iteration */
    fun peek(): T {
        if (!hasNext()) throw NoSuchElementException()
        return next.sure();
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
class CompositeIterator<T>(vararg iterators: java.util.Iterator<T>): AbstractIterator<T>() {

    val iteratorsIter = iterators.iterator()
    var currentIter: java.util.Iterator<T>? = null

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

