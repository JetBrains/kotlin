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

    fun runGenerator(result: Result<Any?> = Result(null)): Any? {
        val suspended = COROUTINE_SUSPENDED
        val stepResult = when (val e = result.exceptionOrNull()) {
            null -> generator.next(result.value)
            else -> generator.throws(e)
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

    override fun resumeWith(result: Result<Any?>) {
        var exception: Throwable? = null
        val nextResult = try {
            runGenerator(result)
        } catch (e: Throwable) {
            exception = e
            null
        }

        if (nextResult === COROUTINE_SUSPENDED) return

        releaseIntercepted()

        resultContinuation?.run {
            if (exception != null) {
                resumeWithException(exception)
            } else {
                resume(nextResult)
            }
        }
    }
}