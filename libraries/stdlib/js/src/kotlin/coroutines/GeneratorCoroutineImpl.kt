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

internal class GeneratorCoroutineImpl(val resultContinuation: Continuation<Any?>?) : InterceptedCoroutine(), Continuation<Any?> {
    var generator: JsIterator<Any?> = VOID.unsafeCast<JsIterator<Any?>>()
    private val _context = resultContinuation?.context

    public override val context: CoroutineContext get() = _context!!

    private fun runGenerator(value: Any?, exception: Throwable?): Any? {
        val suspended = COROUTINE_SUSPENDED
        val stepResult = when (exception) {
            null -> generator.next(value)
            else -> generator.throws(exception)
        }

        var done = stepResult.done
        var value = stepResult.value

        while (!done) {
            try {
                value = value.unsafeCast<() -> Any?>().invoke()
            } catch (e: dynamic) {
                val nextStep = generator.throws(e)
                value = nextStep.value
                done = nextStep.done
                continue
            }
            if (value === suspended) break
            val nextStep = generator.next(value)
            value = nextStep.value
            done = nextStep.done
        }

        return value
    }

    fun runGenerator(result: Result<Any?> = Result(null)): Any? {
        return runGenerator(result.value, result.exceptionOrNull())
    }

    override fun resumeWith(result: Result<Any?>) {
        var current = this
        var currentResult: Any? = result.value
        var currentException: Throwable? = result.exceptionOrNull()

        // This loop unrolls recursion in current.resumeWith(param) to make saner and shorter stack traces on resume
        // It's also fixing the deep recursion case of kotlinx.serialization https://github.com/Kotlin/kotlinx.serialization/issues/1594
        while (true) {
            // Set result and exception fields in the current continuation
            try {
                val outcome = current.runGenerator(currentResult, currentException)
                if (outcome === COROUTINE_SUSPENDED) return
                currentResult = outcome
                currentException = null
            } catch (e: dynamic) {
                currentResult = null
                currentException = e
            }

            current.releaseIntercepted() // this state machine instance is terminating

            when (val completion = current.resultContinuation) {
                null -> return
                is GeneratorCoroutineImpl -> {
                    // unrolling recursion via loop
                    current = completion
                }
                else -> {
                    if (currentException != null) {
                        completion.resumeWithException(currentException)
                    } else {
                        completion.resume(currentResult)
                    }
                    return
                }
            }
        }
    }
}
