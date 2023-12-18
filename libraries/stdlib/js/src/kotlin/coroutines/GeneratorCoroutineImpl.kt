/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines

import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.internal.InlineOnly

// It should be replaced with the regular function generator after the bootstrapping
internal val dummyGenerator = js("""
    // TO PREVENT PREVIOUS VERSIONS OF THE COMPILER FAIL TO COMPILE THE CODE
    var generatorFactory = new Function("return function*(suspended, c) { var a = c(); if (a === suspended) a = yield a; return a }")
    generatorFactory()
""")

internal val GeneratorFunction = dummyGenerator.constructor.prototype

internal fun isGeneratorSuspendStep(value: dynamic): Boolean {
    return value != null && value.constructor === GeneratorFunction
}

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
    private val jsIterators = arrayOf<JsIterator<Any?>>()
    private val _context = resultContinuation?.context

    var isRunning: Boolean = false
    private val unknown: Result<Any?> = Result(js("Symbol()"))
    private var savedResult: Result<Any?> = unknown

    public override val context: CoroutineContext get() = _context!!

    @InlineOnly
    public inline fun dropLastIterator() {
        jsIterators.asDynamic().pop()
    }

    @InlineOnly
    public inline fun addNewIterator(iterator: JsIterator<Any?>) {
        jsIterators.asDynamic().push(iterator)
    }

    @InlineOnly
    private inline val isCompleted: Boolean get() = jsIterators.size == 0

    @InlineOnly
    private inline fun getLastIterator(): JsIterator<Any?> = jsIterators[jsIterators.size - 1]

    @InlineOnly
    public inline fun shouldResumeImmediately(): Boolean = unknown.value !== savedResult.value

    override fun resumeWith(result: Result<Any?>) {
        if (unknown.value === savedResult.value) savedResult = result
        if (isRunning) return

        var currentResult: Any? = savedResult.getOrNull()
        var currentException: Throwable? = savedResult.exceptionOrNull()

        savedResult = unknown

        var current = this

        while (true) {
            while (!current.isCompleted) {
                val jsIterator = current.getLastIterator()
                val exception = currentException.also { currentException = null }

                isRunning = true

                try {
                    val step = when (exception) {
                        null -> jsIterator.next(currentResult)
                        else -> jsIterator.throws(exception)
                    }

                    currentResult = step.value
                    currentException = null

                    if (step.done) current.dropLastIterator()
                    if (unknown.value !== savedResult.value) {
                        currentResult = savedResult.getOrNull()
                        currentException = savedResult.exceptionOrNull()
                        savedResult = unknown
                    } else if (currentResult === COROUTINE_SUSPENDED) return
                } catch (e: Throwable) {
                    currentException = e
                    current.dropLastIterator()
                } finally {
                    isRunning = false
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