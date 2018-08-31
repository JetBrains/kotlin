/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package kotlin.coroutines.experimental

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import kotlin.*
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED

@PublishedApi
internal actual class SafeContinuation<in T>
internal actual constructor(
    private val delegate: Continuation<T>,
    initialResult: Any?
) : Continuation<T> {

    @PublishedApi
    internal actual constructor(delegate: Continuation<T>) : this(delegate, UNDECIDED)

    public actual override val context: CoroutineContext
        get() = delegate.context

    @Volatile
    private var result: Any? = initialResult

    companion object {
        private val UNDECIDED: Any? = Any()
        private val RESUMED: Any? = Any()

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        private val RESULT = AtomicReferenceFieldUpdater.newUpdater<SafeContinuation<*>, Any?>(
            SafeContinuation::class.java, Any::class.java as Class<Any?>, "result"
        )
    }

    private class Fail(val exception: Throwable)

    actual override fun resume(value: T) {
        while (true) { // lock-free loop
            val result = this.result // atomic read
            when {
                result === UNDECIDED -> if (RESULT.compareAndSet(this, UNDECIDED, value)) return
                result === COROUTINE_SUSPENDED -> if (RESULT.compareAndSet(this, COROUTINE_SUSPENDED, RESUMED)) {
                    delegate.resume(value)
                    return
                }
                else -> throw IllegalStateException("Already resumed")
            }
        }
    }

    actual override fun resumeWithException(exception: Throwable) {
        while (true) { // lock-free loop
            val result = this.result // atomic read
            when {
                result === UNDECIDED -> if (RESULT.compareAndSet(this, UNDECIDED, Fail(exception))) return
                result === COROUTINE_SUSPENDED -> if (RESULT.compareAndSet(this, COROUTINE_SUSPENDED, RESUMED)) {
                    delegate.resumeWithException(exception)
                    return
                }
                else -> throw IllegalStateException("Already resumed")
            }
        }
    }

    @PublishedApi
    internal actual fun getResult(): Any? {
        var result = this.result // atomic read
        if (result === UNDECIDED) {
            if (RESULT.compareAndSet(this, UNDECIDED, COROUTINE_SUSPENDED)) return COROUTINE_SUSPENDED
            result = this.result // reread volatile var
        }
        when {
            result === RESUMED -> return COROUTINE_SUSPENDED // already called continuation, indicate COROUTINE_SUSPENDED upstream
            result is Fail -> throw result.exception
            else -> return result // either COROUTINE_SUSPENDED or data
        }
    }
}
