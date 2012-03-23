package kotlin.support

import java.util.*

// TODO should we use enums?
private val Ready = 0
private val NotReady = 1
private val Done = 2
private val Failed = 3

/**
 * A base class to simplify implementing iterators so that implementations only have to implement [[#computeNext()]]
 * to implement the iterator, calling [[done()]] when the iteration is complete.
 */
abstract class AbstractIterator<T>: Iterator<T> {
    var state = NotReady
    var next: T? = null

    override fun hasNext(): Boolean {
        require(state != Failed)
        return when (state) {
            Done -> false
            Ready -> true
            else -> tryToComputeNext()
        }
    }

    override fun next(): T {
        if (!hasNext()) {
            throw NoSuchElementException();
        } else {
            state = NotReady
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
        state = Failed
        next = computeNext();
        return if (state != Done) {
            state = Ready
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
        state = Done
    }
}