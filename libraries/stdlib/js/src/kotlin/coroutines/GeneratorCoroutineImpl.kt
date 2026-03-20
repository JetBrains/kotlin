/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines

import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

internal external interface JsIterationStep<T> {
    val done: Boolean
    val value: T
}

internal external interface JsIterator<T> {
    fun next(value: Any? = definedExternally): JsIterationStep<T>

    @JsName("throw")
    fun throws(exception: Throwable = definedExternally): JsIterationStep<T>
}

internal class GeneratorCoroutineImpl(val resultContinuation: Continuation<Any?>) : InterceptedCoroutine(), Continuation<Any?> {
    companion object {
        private val WAITING_FOR_UNEXPECTED_RESUMING = js("Symbol()")
    }

    var generator: JsIterator<Any?> = VOID.unsafeCast<JsIterator<Any?>>()
    private val _context = resultContinuation.context

    public override val context: CoroutineContext get() = _context

    fun runGenerator(value: Any? = null, exception: Throwable? = null): Any? {
        val stepResult = when (exception) {
            null -> generator.next(value)
            else -> generator.throws(exception)
        }

        return if (stepResult.done) stepResult.value else COROUTINE_SUSPENDED
    }

    internal var savedResult: Any? = null

    internal fun waitingForUnexpectedResuming() {
        savedResult = WAITING_FOR_UNEXPECTED_RESUMING
    }

    internal fun getOrThrow(): Any? {
        return when (val result = savedResult) {
            WAITING_FOR_UNEXPECTED_RESUMING -> {
                savedResult = null
                COROUTINE_SUSPENDED
            }
            is Result.Failure -> throw result.exception
            else -> result
        }
    }

    override fun resumeWith(result: Result<Any?>) {
        if (savedResult === WAITING_FOR_UNEXPECTED_RESUMING) {
            savedResult = result.value
            return
        }

        var current = this
        var currentResult: Any? = result.getOrNull()
        var currentException: Throwable? = result.exceptionOrNull()

        // This loop unrolls recursion in current.resumeWith(param) to make saner and shorter stack traces on resume
        while (true) {
            try {
                currentResult = current.runGenerator(currentResult, currentException)
                currentException = null
            } catch (e: dynamic) {
                currentResult = null
                currentException = e.unsafeCast<Throwable>()
            }

            if (currentResult === COROUTINE_SUSPENDED) return

            current.releaseIntercepted()

            val nextContinuation = current.resultContinuation

            if (nextContinuation is GeneratorCoroutineImpl) {
                current = nextContinuation
            } else {
                if (currentException != null) {
                    nextContinuation.resumeWithException(currentException)
                } else {
                    nextContinuation.resume(currentResult)
                }
                return
            }
        }
    }
}