package org.jetbrains.kotlin.tools.projectWizard.core

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume

interface ComputeContextState

object NoState : ComputeContextState

data class StateContainer<S : ComputeContextState>(var state: S)

open class ComputeContext<S : ComputeContextState> protected constructor(private val stateContainer: StateContainer<S>) {
    private var result: TaskResult<Any>? = null
    private var exception: Throwable? = null

    val state
        get() = stateContainer.state

    suspend fun <T : Any> TaskResult<T>.get(): T = suspendCoroutineUninterceptedOrReturn { continuation ->
        result = this
        if (this is Success<T>) {
            continuation.resume(value)
        }
        COROUTINE_SUSPENDED
    }

    suspend operator fun <T : Any> TaskResult<T>.component1(): T = get()

    suspend fun <T : Any> TaskResult<T>.ensure() {
        get()
    }

    suspend fun fail(error: Error): Nothing = suspendCoroutineUninterceptedOrReturn {
        result = Failure(error)
        COROUTINE_SUSPENDED
    }

    suspend fun fail(errors: List<Error>): Nothing = suspendCoroutineUninterceptedOrReturn {
        result = Failure(errors)
        COROUTINE_SUSPENDED
    }

    suspend inline fun <T : Any> TaskResult<T>?.nullableValue(): T? =
        this?.onFailure { fail(it) }?.asNullable

    fun <T : Any> compute(
        block: suspend ComputeContext<S>.() -> T
    ): TaskResult<T> = computeM { success(block()) }

    fun <T : Any> computeM(
        block: suspend ComputeContext<S>.() -> TaskResult<T>
    ): TaskResult<T> {
        val computeContext = ComputeContext(stateContainer)
        val continuation = object : Continuation<TaskResult<T>> {
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            override fun resumeWith(result: Result<TaskResult<T>>) {
                result.onSuccess { computeContext.result = it }
                result.onFailure { computeContext.exception = it }
            }
        }
        block.createCoroutineUnintercepted(computeContext, continuation).resume(Unit)
        if (computeContext.exception != null) {
            throw computeContext.exception!!
        }
        @Suppress("UNCHECKED_CAST") return computeContext.result as TaskResult<T>
    }

    fun <A, B : Any> Iterable<A>.mapCompute(
        f: suspend ComputeContext<S>.(A) -> B
    ): List<TaskResult<B>> = map { compute { f(it) } }

    fun <A, B : Any> Iterable<A>.mapComputeM(
        f: suspend ComputeContext<S>.(A) -> TaskResult<B>
    ): List<TaskResult<B>> = map { computeM { f(it) } }


    fun updateState(updater: (S) -> S) {
        stateContainer.state = updater(stateContainer.state)
    }

    companion object {
        val PURE = ComputeContext(StateContainer(NoState))

        fun <S : ComputeContextState, R : Any> runInComputeContextWithState(
            state: S,
            action: ComputeContext<S>.() -> TaskResult<R>
        ): TaskResult<Pair<R, S>> = with(ComputeContext(StateContainer(state))) {
            val result = action()
            result.map { it to this.state }
        }
    }
}


fun <T : Any> compute(
    block: suspend ComputeContext<NoState>.() -> T
): TaskResult<T> = ComputeContext.PURE.compute(block)


fun <T : Any> computeM(
    block: suspend ComputeContext<NoState>.() -> TaskResult<T>
): TaskResult<T> = ComputeContext.PURE.computeM(block)


fun <A, B : Any> Iterable<A>.mapCompute(
    f: suspend ComputeContext<NoState>.(A) -> B
): List<TaskResult<B>> = with(ComputeContext.PURE) {
    mapCompute(f)
}

fun <A, B : Any> Iterable<A>.mapComputeM(
    f: suspend ComputeContext<NoState>.(A) -> TaskResult<B>
): List<TaskResult<B>> = with(ComputeContext.PURE) {
    mapComputeM(f)
}
