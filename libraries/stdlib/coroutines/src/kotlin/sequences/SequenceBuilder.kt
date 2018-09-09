/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("SequenceBuilderKt")

package kotlin.sequences

import kotlin.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

/**
 * Builds a [Sequence] lazily yielding values one by one.
 *
 * @see kotlin.sequences.generateSequence
 *
 * @sample samples.collections.Sequences.Building.buildSequenceYieldAll
 * @sample samples.collections.Sequences.Building.buildFibonacciSequence
 */
@SinceKotlin("1.3")
public fun <T> buildSequence(builderAction: suspend SequenceBuilder<T>.() -> Unit): Sequence<T> = Sequence { buildIterator(builderAction) }

/**
 * Builds an [Iterator] lazily yielding values one by one.
 *
 * @sample samples.collections.Sequences.Building.buildIterator
 * @sample samples.collections.Iterables.Building.iterable
 */
@SinceKotlin("1.3")
public fun <T> buildIterator(builderAction: suspend SequenceBuilder<T>.() -> Unit): Iterator<T> {
    val iterator = SequenceBuilderIterator<T>()
    iterator.nextStep = builderAction.createCoroutineUnintercepted(receiver = iterator, completion = iterator)
    return iterator
}

/**
 * Builder for a [Sequence] or an [Iterator], provides [yield] and [yieldAll] suspension functions.
 *
 * @see buildSequence
 * @see buildIterator
 *
 * @sample samples.collections.Sequences.Building.buildSequenceYieldAll
 * @sample samples.collections.Sequences.Building.buildFibonacciSequence
 */
@RestrictsSuspension
@SinceKotlin("1.3")
public abstract class SequenceBuilder<in T> internal constructor() {
    /**
     * Yields a value to the [Iterator] being built.
     *
     * @sample samples.collections.Sequences.Building.buildSequenceYieldAll
     * @sample samples.collections.Sequences.Building.buildFibonacciSequence
     */
    public abstract suspend fun yield(value: T)

    /**
     * Yields all values from the `iterator` to the [Iterator] being built.
     *
     * The sequence of values returned by the given iterator can be potentially infinite.
     *
     * @sample samples.collections.Sequences.Building.buildSequenceYieldAll
     */
    public abstract suspend fun yieldAll(iterator: Iterator<T>)

    /**
     * Yields a collections of values to the [Iterator] being built.
     *
     * @sample samples.collections.Sequences.Building.buildSequenceYieldAll
     */
    public suspend fun yieldAll(elements: Iterable<T>) {
        if (elements is Collection && elements.isEmpty()) return
        return yieldAll(elements.iterator())
    }

    /**
     * Yields potentially infinite sequence of values  to the [Iterator] being built.
     *
     * The sequence can be potentially infinite.
     *
     * @sample samples.collections.Sequences.Building.buildSequenceYieldAll
     */
    public suspend fun yieldAll(sequence: Sequence<T>) = yieldAll(sequence.iterator())
}

private typealias State = Int

private const val State_NotReady: State = 0
private const val State_ManyNotReady: State = 1
private const val State_ManyReady: State = 2
private const val State_Ready: State = 3
private const val State_Done: State = 4
private const val State_Failed: State = 5

private class SequenceBuilderIterator<T> : SequenceBuilder<T>(), Iterator<T>, Continuation<Unit> {
    private var state = State_NotReady
    private var nextValue: T? = null
    private var nextIterator: Iterator<T>? = null
    var nextStep: Continuation<Unit>? = null

    override fun hasNext(): Boolean {
        while (true) {
            when (state) {
                State_NotReady -> {}
                State_ManyNotReady ->
                    if (nextIterator!!.hasNext()) {
                        state = State_ManyReady
                        return true
                    } else {
                        nextIterator = null
                    }
                State_Done -> return false
                State_Ready, State_ManyReady -> return true
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
            State_NotReady, State_ManyNotReady -> return nextNotReady()
            State_ManyReady -> {
                state = State_ManyNotReady
                return nextIterator!!.next()
            }
            State_Ready -> {
                state = State_NotReady
                @Suppress("UNCHECKED_CAST")
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


    override suspend fun yield(value: T) {
        nextValue = value
        state = State_Ready
        return suspendCoroutineUninterceptedOrReturn { c ->
            nextStep = c
            COROUTINE_SUSPENDED
        }
    }

    override suspend fun yieldAll(iterator: Iterator<T>) {
        if (!iterator.hasNext()) return
        nextIterator = iterator
        state = State_ManyReady
        return suspendCoroutineUninterceptedOrReturn { c ->
            nextStep = c
            COROUTINE_SUSPENDED
        }
    }

    // Completion continuation implementation
    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow() // just rethrow exception if it is there
        state = State_Done
    }

    override val context: CoroutineContext
        get() = EmptyCoroutineContext
}
