package kotlin.support

import java.util.NoSuchElementException

public enum class State {
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
    public var state: State = State.NotReady
    public var next: T? = null

    public override fun hasNext(): Boolean {
        require(state != State.Failed)
        return when (state) {
            State.Done -> false
            State.Ready -> true
            else -> tryToComputeNext()
        }
    }

    public override fun next(): T {
        if (!hasNext()) throw NoSuchElementException()
        state = State.NotReady
        return next.sure()
    }

    public override fun remove() { throw UnsupportedOperationException() }

    /** Returns the next element in the iteration without advancing the iteration */
    fun peek(): T {
        if (!hasNext()) throw NoSuchElementException()
        return next.sure();
    }

    private fun tryToComputeNext(): Boolean {
        state = State.Failed
        next = computeNext();
        return if (state != State.Done) { state = State.Ready; true } else false
    }

    /** Computes the next element in the iterator, calling [[done()]] when there are no more elements */
    abstract protected fun computeNext(): T?

    /** Sets the state to done so that the iteration terminates */
    protected fun done() {
        state = State.Done
    }
}

/** An [[Iterator]] which invokes a function to calculate the next value in the iteration until the function returns *null* */
class FunctionIterator<T>(val nextFunction : () -> T?) : AbstractIterator<T>() {

    override protected fun computeNext(): T? {
        val next = (nextFunction)()
        if (next == null) done()
        return next
    }
}
