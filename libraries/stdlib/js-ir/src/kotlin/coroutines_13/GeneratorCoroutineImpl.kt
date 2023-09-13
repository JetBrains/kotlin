/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines

import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

private val GeneratorFunction = js("(function*(){}).constructor.prototype")

external interface JsIterationStep<T> {
    val done: Boolean
    val value: T
}

external interface JsIterator<T> {
    fun next(value: Any?): JsIterationStep<T>

    @JsName("throw")
    fun throws(exception: Throwable): JsIterationStep<T>
}

internal class GeneratorCoroutineImpl(
    generator: (Continuation<Any?>) -> JsIterator<Any?>,
    private val resultContinuation: Continuation<Any?>?
) : InterceptedCoroutine(), Continuation<Any?> {
    private val _context = resultContinuation?.context

    private val instantiatedGenerators = arrayOf(generator(this))

    public override val context: CoroutineContext get() = _context!!

    @Suppress("NOTHING_TO_INLINE")
    private inline fun dropLastGenerator() = instantiatedGenerators.asDynamic().pop();

    @Suppress("NOTHING_TO_INLINE")
    private inline fun addNewGenerator(generator: JsIterator<Any?>) = instantiatedGenerators.asDynamic().push(generator);

    private inline fun Any?.asJsIterator(block: JsIterator<Any?>.() -> Unit) {
        val value = asDynamic()
        if (value?.constructor === GeneratorFunction) {
            value.unsafeCast<JsIterator<Any?>>().block()
        }
    }

    override fun resumeWith(result: Result<Any?>) {
        var current = this
        var currentResult: Any? = result.getOrNull()
        var currentException: Throwable? = result.exceptionOrNull()

        while (true) {
            while (current.instantiatedGenerators.size > 0) {
                val generator = current.instantiatedGenerators[current.instantiatedGenerators.size - 1]
                val exception = currentException.also { currentException = null }
                try {
                    val step = when (exception) {
                        null -> generator.next(currentResult)
                        else -> generator.throws(exception)
                    }

                    currentResult = step.value

                    if (step.done) current.dropLastGenerator()
                    if (currentResult === COROUTINE_SUSPENDED) return

                    currentResult.asJsIterator { current.addNewGenerator(this) }
                } catch (e: Throwable) {
                    currentException = e
                    current.dropLastGenerator()
                }
            }

            releaseIntercepted()

            val completion = resultContinuation!!

            if (completion is GeneratorCoroutineImpl) {
                current = completion
            } else {
                return if (currentException != null) {
                    completion.resumeWithException(currentException!!)
                } else {
                    completion.resume(currentResult)
                }
            }
        }
    }
}