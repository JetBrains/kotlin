@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("SequencesKt")
package kotlin.sequences

import kotlin.coroutines.*

/**
 *  Builds a [Sequence] lazily yielding values one by one.
 */
public fun <T> buildSequence(builderAction: suspend SequenceBuilder<T>.() -> Unit): Sequence<T> = Sequence { buildIteratorImpl(builderAction) }


/**
 * Builder for a [Sequence] or an [Iterator], provides [yield] and [yieldAll] suspension functions.
 */
@RestrictsSuspension
public abstract class SequenceBuilder<in T> internal constructor() {
    /**
     * Yields a value to the [Iterator] being built.
     */
    public abstract suspend fun yield(value: T)

    /**
     * Yields all values from the `iterator` to the [Iterator] being built.
     *
     * The sequence of values returned by the given iterator can be potentially infinite.
     */
    public abstract suspend fun yieldAll(iterator: Iterator<T>)

    /**
     * Yields a collections of values to the [Iterator] being built.
     */
    public suspend fun yieldAll(elements: Iterable<T>) {
        if (elements is Collection && elements.isEmpty()) return
        return yieldAll(elements.iterator())
    }

    /**
     * Yields potentially infinite sequence of values  to the [Iterator] being built.
     *
     * The sequence can be potentially infinite.
     */
    public suspend fun yieldAll(sequence: Sequence<T>) = yieldAll(sequence.iterator())
}



internal fun <T> buildIteratorImpl(builderAction: suspend SequenceBuilder<T>.() -> Unit): Iterator<T> {
    val iterator = YieldingIterator<T>()
    iterator.nextStep = builderAction.createCoroutine(receiver = iterator.builder, completion = iterator)
    return iterator
}




private typealias State = Int
private const val State_Failed: State = 0
private const val State_Done: State = 1
private const val State_Ready: State = 2
private const val State_ManyReady: State = 3
private const val State_NotReady: State = 4

/**
 * A base class to simplify implementing iterators so that implementations only have to implement [computeNext]
 * to implement the iterator, calling [done] when the iteration is complete.
 */
// TODO: Merge to kotlin.collections.AbstractIterator
private abstract class AbstractIterator<T>: Iterator<T> {
    private var state = State_NotReady
    private var nextValue: Any? = null

    override fun hasNext(): Boolean {
        require(state != State_Failed)
        while (true) {
            when (state) {
                State_Failed,
                State_Done -> return false
                State_Ready -> return true
                State_ManyReady -> if ((nextValue as Iterator<*>).hasNext()) return true
                State_NotReady -> {}
                else -> throw unexpectedState()
            }

            state = State_Failed
            computeNext()
        }
    }

    override fun next(): T {
        if (!hasNext()) throw NoSuchElementException()
        when (state) {
            State_Ready -> {
                state = State_NotReady
                return nextValue as T
            }
            State_ManyReady -> {
                val iterator = nextValue as Iterator<T>
                return iterator.next()
            }
            else -> throw unexpectedState()
        }
    }

    private fun unexpectedState(): Throwable = IllegalStateException("Unexpected state of the iterator: $state")


    /**
     * Computes the next item in the iterator.
     *
     * This callback method should call one of these two methods:
     *
     * * [setNext] with the next value of the iteration
     * * [done] to indicate there are no more elements
     *
     * Failure to call either method will result in the iteration terminating with a failed state
     */
    abstract protected fun computeNext(): Unit

    /**
     * Sets the next value in the iteration, called from the [computeNext] function
     */
    protected fun setNext(value: T): Unit {
        nextValue = value
        state = State_Ready
    }

    /**
     * Sets the iterator to retrieve the next values from, called from the [computeNext] function
     */
    protected fun setNextMultiple(iterator: Iterator<T>): Unit {
        nextValue = iterator
        state = State_ManyReady
    }

    /**
     * Sets the state to done so that the iteration terminates.
     */
    protected fun done() {
        state = State_Done
    }
}




private class YieldingIterator<T> : AbstractIterator<T>(), Continuation<Unit> {
    var nextStep: Continuation<Unit>? = null

    override fun computeNext() {
        val step = nextStep!!
        nextStep = null
        step.resume(Unit)
    }

    val builder: SequenceBuilder<T> = object : SequenceBuilder<T>() {
        suspend override fun yield(value: T) {
            setNext(value)
            return CoroutineIntrinsics.suspendCoroutineOrReturn { c ->
                nextStep = c
                CoroutineIntrinsics.SUSPENDED
            }
        }

        suspend override fun yieldAll(iterator: Iterator<T>) {
            if (!iterator.hasNext()) return
            setNextMultiple(iterator)
            return CoroutineIntrinsics.suspendCoroutineOrReturn { c ->
                nextStep = c
                CoroutineIntrinsics.SUSPENDED
            }
        }
    }

    // Completion continuation implementation
    override fun resume(value: Unit) {
        done()
    }

    override fun resumeWithException(exception: Throwable) {
        throw exception // just rethrow
    }

}
