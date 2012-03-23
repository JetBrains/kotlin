package kotlin.support

import java.util.*

enum class State {
    Ready
    NotReady
    Done
    Failed
}

/**
 * A base class to simplify implementing iterators so that implementations only have to implement [[#computeNext()]]
 * to implement the iterator, calling [[done()]] when the iteration is complete.
 */
abstract class AbstractIterator<T>: java.util.Iterator<T> {
    var state: State = State.NotReady
    var next: T? = null

    override fun hasNext(): Boolean {
        require(state != State.Failed)
        return when (state) {
            State.Done -> false
            State.Ready -> true
            else -> tryToComputeNext()
        }
    }

    override fun next(): T {
        if (!hasNext()) {
            throw NoSuchElementException();
        } else {
            state = State.NotReady
            return next.sure()
        }
    }

    override fun remove() {
        throw UnsupportedOperationException()
    }

    /**
    * Returns the next element in the iteration without advancing the iteration
    */
    fun peek(): T {
        if (!hasNext()) {
            throw NoSuchElementException();
        }
        return next.sure();
    }

    private fun tryToComputeNext(): Boolean {
        state = State.Failed
        next = computeNext();
        return if (state != State.Done) {
            state = State.Ready
            true
        } else false
    }

    /**
     * Computes the next element in the iterator, calling endOfData() when
     * there are no more elements
     */
    abstract protected fun computeNext(): T?

    /**
     * Sets the state to done so that the iteration terminates
     */
    protected fun done() {
        state = State.Done
    }
}