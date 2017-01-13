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
    iterator.nextStep = builderAction.createCoroutine(receiver = iterator, completion = iterator)
    return iterator
}




private typealias State = Int
private const val State_NotReady: State = 0
private const val State_ManyReady: State = 1
private const val State_Ready: State = 2
private const val State_Done: State = 3
private const val State_Failed: State = 4


private class YieldingIterator<T> : SequenceBuilder<T>(), Iterator<T>, Continuation<Unit> {
    private var state = State_NotReady
    private var nextValue: T? = null
    private var nextIterator: Iterator<T>? = null
    var nextStep: Continuation<Unit>? = null

    override fun hasNext(): Boolean {
        while (true) {
            when (state) {
                State_NotReady -> {}
                State_ManyReady ->
                    if (nextIterator!!.hasNext()) return true else nextIterator = null
                State_Done -> return false
                State_Ready -> return true
                else -> throw exceptionalState()
            }

            state = State_Failed
            val step = nextStep!!
            nextStep = null
            step.resume(Unit)
        }
    }

    override fun next(): T {
        when (state) {
            State_NotReady -> return nextNotReady()
            State_ManyReady -> return nextIterator!!.next()
            State_Ready -> {
                state = State_NotReady
                val result = nextValue as T
                nextValue = null
                return result
            }
            else -> throw exceptionalState()
        }
    }

    private fun nextNotReady(): T {
        if (!hasNext()) throw NoSuchElementException() else return next()
    }

    private fun exceptionalState(): Throwable = when (state) {
        State_Done -> NoSuchElementException()
        State_Failed -> IllegalStateException("Iterator has failed.")
        else -> IllegalStateException("Unexpected state of the iterator: $state")
    }


    suspend override fun yield(value: T) {
        nextValue = value
        state = State_Ready
        return CoroutineIntrinsics.suspendCoroutineOrReturn { c ->
            nextStep = c
            CoroutineIntrinsics.SUSPENDED
        }
    }

    suspend override fun yieldAll(iterator: Iterator<T>) {
        if (!iterator.hasNext()) return
        nextIterator = iterator
        state = State_ManyReady
        return CoroutineIntrinsics.suspendCoroutineOrReturn { c ->
            nextStep = c
            CoroutineIntrinsics.SUSPENDED
        }
    }

    // Completion continuation implementation
    override fun resume(value: Unit) {
        state = State_Done
    }

    override fun resumeWithException(exception: Throwable) {
        throw exception // just rethrow
    }
}
